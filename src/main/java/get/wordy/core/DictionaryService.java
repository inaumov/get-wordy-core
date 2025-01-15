package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.IVocabularyService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.exception.CardNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.core.dao.impl.VocabularyDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.exception.VocabNotFoundException;
import get.wordy.core.api.bean.wrapper.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DictionaryService implements IDictionaryService, IVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryService.class);

    private VocabularyDao vocabularyDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private CardHeadlineDao cardHeadlineDao;
    private LocalTxManager connection;
    private final Map<OwnerId, List<Vocabulary>> dictionariesCache = new HashMap<>();
    private final Map<Integer, Card> cardsCache = new HashMap<>();
    private final Map<Integer, List<Word>> wordsCache = new HashMap<>();

    @SuppressWarnings("unused")
    public DictionaryService() {
    }

    @SuppressWarnings("unused")
    public DictionaryService(VocabularyDao vocabularyDao,
                             WordDao wordDao,
                             CardDao cardDao,
                             CardHeadlineDao cardHeadlineDao,
                             LocalTxManager connection
    ) {
        this.vocabularyDao = vocabularyDao;
        this.wordDao = wordDao;
        this.cardDao = cardDao;
        this.cardHeadlineDao = cardHeadlineDao;
        this.connection = connection;
    }

    @Override
    public List<Vocabulary> getVocabularies(OwnerId ownerId) {

        List<Vocabulary> vocabularies = dictionariesCache.get(ownerId);
        if (vocabularies != null && !vocabularies.isEmpty()) {
            return vocabularies;
        }

        List<Vocabulary> list;
        try {
            // fetch from the database if not present in the cache
            list = vocabularyDao.selectAllByOwnerId(ownerId);

            // update the cache
            dictionariesCache.put(ownerId, list);
        } catch (DataAccessException e) {
            LOG.error("Error while loading dictionaries", e);
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public Vocabulary createVocabulary(OwnerId ownerId, String name, String pictureUrl) {
        Vocabulary vocabulary = new Vocabulary(name, pictureUrl);
        try {
            connection.open();
            final Vocabulary saved = vocabularyDao.insert(ownerId, vocabulary);
            connection.commit();
            putToCache(ownerId, () -> saved);
            return vocabulary;
        } catch (DaoException | DataAccessException e) {
            LOG.error("Error while creating a new vocabulary", e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean renameVocabulary(OwnerId ownerId, int vocabId, String newName) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.getName(), newName)) {
            return true;
        }
        // do modification
        try {
            connection.open();
            vocabularyDao.rename(vocabId, newName);
            connection.commit();
            vocabulary.setName(newName); // should update name in cache
        } catch (DaoException e) {
            LOG.error("Error while renaming vocabulary, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean changeVocabularyPicture(OwnerId ownerId, int vocabId, String newPictureUrl) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.getPictureUrl(), newPictureUrl)) {
            return true;
        }
        try {
            connection.open();
            vocabularyDao.updatePicture(vocabId, newPictureUrl);
            connection.commit();
            vocabulary.setPictureUrl(newPictureUrl); // should update pic url in cache
        } catch (DaoException e) {
            LOG.error("Error while changing vocabulary picture, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean makeVocabularyIsShared(OwnerId ownerId, int vocabId, boolean isShared) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.isShared(), isShared)) {
            return true;
        }
        // do modifications
        try {
            connection.open();
            int updated = vocabularyDao.updateIsShared(vocabId, isShared);
            connection.commit();
            if (updated > 0) {
                vocabulary.setShared(isShared); // should update readiness url in cache
            }
        } catch (DaoException e) {
            LOG.error("Error while updating vocabulary, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean deleteVocabulary(OwnerId ownerId, int vocabId) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (vocabulary.getWordsTotal() > 0) {
            throw new DictionaryServiceException("Cannot delete vocabulary with words (not empty)");
        }
        // do action
        try {
            connection.open();
            vocabularyDao.deleteVocabularyById(vocabId);
            connection.commit();
            dictionariesCache.get(ownerId)
                    .remove(vocabulary);
        } catch (DaoException | DataAccessException e) {
            connection.rollback();
            LOG.error("Error while removing vocabulary by id = {}", vocabId, e);
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public List<Word> getWords(OwnerId ownerId, int vocabId) {
        if (wordsCache.containsKey(vocabId)) {
            List<Word> wordsheetItems = wordsCache.get(vocabId);
            if (!CollectionUtils.isEmpty(wordsheetItems)) {
                return wordsheetItems;
            }
        }
        List<Word> vocabulary;
        try {
            connection.open();
            Set<Integer> wordsRefs = vocabularyDao.getWordRefs(findVocab(ownerId, vocabId).getVocabId());
            vocabulary = wordDao.selectAll(wordsRefs);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all words for vocabulary id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }

        wordsCache.put(vocabId, vocabulary);

        return List.copyOf(vocabulary);
    }

    @Override
    public List<Card> getCards(OwnerId ownerId, int dictionaryId) {
        List<Card> cardListFull;
        try {
            connection.open();
            cardListFull = cardHeadlineDao.getCardsForDictionary(findVocab(ownerId, dictionaryId).getVocabId());
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all cards in dictionary by id = {}", dictionaryId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }

        Map<Integer, Card> cardsMap = cardListFull
                .stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        cardsCache.clear();
        cardsCache.putAll(cardsMap);

        return List.copyOf(cardListFull);
    }

    @Override
    public List<Exercise> getCardsForExercise(OwnerId ownerId, int dictionaryId, int limit) {
        List<Exercise> exercises = new ArrayList<>();
        try {
            connection.open();
            int[] cardIds = cardDao.selectCardIdsForExercise(dictionaryId, limit);
            LOG.info("Selected card ids for exercise from database = {}", cardIds);
            if (cardIds == null || cardIds.length == 0) {
                return Collections.emptyList();
            }

            // check if cards are in the cache
            int[] cardsInCache = IntStream.of(cardIds)
                    .filter(cardsCache::containsKey)
                    .toArray();
            boolean allCached = cardsInCache.length == cardIds.length;

            if (allCached) { // get only actual sentences from database for cached cards
                LOG.debug("Get actual sentences from database for cached cards = {}", cardsInCache);
                Map<Integer, List<Sentence>> missingSentences = cardHeadlineDao.getSentencesFor(cardIds);
                connection.commit();
                for (Integer id : cardIds) {
                    Card card = cardsCache.get(id);
                    Exercise exercise = new Exercise();
                    exercise.setCardId(card.getId());
                    exercise.setWordId(card.getWord().getId());
                    exercise.setWord(card.getWord());
                    Optional.ofNullable(missingSentences.get(id))
                            .ifPresent(exercise::setSentences);
                    exercises.add(exercise);
                }
            } else { // get all in case NOT fully present in cache
                LOG.debug("Get cards for exercise from database for ids = {}", cardIds);
                List<Exercise> cardsForExercise = cardHeadlineDao.getCardsForExercise(cardIds);
                connection.commit();
                exercises.addAll(cardsForExercise);
            }
        } catch (DaoException e) {
            LOG.error("Error while loading exercise cards set for dictionary, id = {}", dictionaryId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return exercises;
    }

    @Override
    public Card addCard(int dictionaryId, Card card) {
        try {
            connection.open();
            Word word = wordDao.insert(card.getWord());
            card.setDictionaryId(dictionaryId);
            card.setWordId(word.getId());
            card.setWord(word);
            if (isReadyToLearn(word)) {
                card.setStatus(CardStatus.TO_LEARN);
            }
            Card insertedCard = cardDao.insert(card);

            connection.commit();
            int cardId = insertedCard.getId();
            if (cardId > 0) {
                cardsCache.put(cardId, insertedCard);
                return insertedCard;
            } else {
                throw new DictionaryServiceException();
            }
        } catch (DaoException e) {
            LOG.error("Error while saving a card into dictionary", e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
    }

    private static boolean isReadyToLearn(Word word) {
        return StringUtils.hasText(word.getValue())
                && StringUtils.hasText(word.getPartOfSpeech())
                && StringUtils.hasText(word.getMeaning());
    }

    @Override
    public Card updateCard(int dictionaryId, Card card) {
        int cardId = card.getId();
        // needed for equality test
        card.setDictionaryId(dictionaryId);

        Card cachedCard = loadFullCardHeadlineFromDb(cardId);
        Word cachedWord = cachedCard.getWord();

        Word word = card.getWord();
        boolean sameWord = Objects.equals(word, cachedWord);
        boolean sameCard = Objects.equals(card, cachedCard);

        if (sameWord && sameCard) {
            LOG.debug("Nothing to update in a card {} from dictionary {}. Return", cardId, dictionaryId);
            return card;
        }

        try {
            connection.open();
            if (!sameWord) {
                int updated = wordDao.update(word);
                // sync
                if (updated == 0) {
                    LOG.warn("Nothing has been updated for card id = {}", card.getId());
                }
            }
            if (!sameCard) {
                card = cardDao.updateRelations(card);
            }
            connection.commit();

        } catch (DaoException e) {
            LOG.error("Error while updating card, id = {}", cardId, e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }

        // refresh in cache
        cardsCache.put(cardId, card);

        return card;
    }

    @Override
    public boolean deleteCard(OwnerId ownerId, int dictionaryId, int cardId) {
        findVocab(ownerId, dictionaryId);
        Card card = findCardById(cardId);
        try {
            connection.open();

            cardDao.delete(cardId);
            wordDao.delete(card.getWordId());
            connection.commit();
            cardsCache.remove(cardId);
        } catch (DaoException e) {
            LOG.error("Error while removing card by id = {}", cardId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public Card loadCard(int cardId) {
        return Optional.ofNullable(cardsCache.get(cardId))
                .orElseGet(() -> loadFullCardHeadlineFromDb(cardId));
    }

    private Card loadFullCardHeadlineFromDb(int cardId) {
        Card card;
        try {
            connection.open();
            card = cardHeadlineDao.getCardById(cardId);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading a card headline with id = {}", cardId, e);
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
        if (card == null) {
            throw new CardNotFoundException();
        }
        cardsCache.put(cardId, card);
        return card;
    }

    @Override
    public boolean changeStatus(int cardId, CardStatus newStatus) {
        Card card = findCardById(cardId);
        if (card == null) {
            return false;
        }
        card.setStatus(newStatus);

        if (newStatus == CardStatus.LEARNT) {
            card.setScore(100);
        } else if (newStatus == CardStatus.EDIT) {
            card.setScore(0);
        }

        try {
            connection.open();
            cardDao.updateStatus(cardId, newStatus, card.getScore());
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while changing card status, id = {}", cardId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public Score getScoreSummary(OwnerId ownerId, int dictionaryId) {
        try {
            Vocabulary vocabulary = findVocab(ownerId, dictionaryId);
            Score score = new Score();
            connection.open();
            Map<String, Integer> result = cardDao.getScoreSummary(vocabulary.getVocabId());
            connection.commit();
            Set<String> statuses = result.keySet();
            for (String status : statuses) {
                score.setScoreCount(CardStatus.valueOf(status), result.get(status));
            }
            return score;
        } catch (DaoException e) {
            LOG.error("Error while getting total score summary for dictionary, id = {}", dictionaryId, e);
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean resetScore(int cardId) {
        // todo: check permission to dictionary
        try {
            connection.open();
            cardDao.resetScore(cardId, CardStatus.TO_LEARN);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while resetting score for card, id = {}", cardId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean increaseScoreUp(final int dictionaryId, int[] cardIds, int repetitions) {
        // omit duplicates if any
        int[] uniqueCardIds = Arrays.stream(cardIds)
                .distinct()
                .toArray();
        List<Card> cards = new ArrayList<>();
        for (int cardId : uniqueCardIds) {
            Card card = findCardById(cardId);

            int diff = 100 / repetitions;
            int score = card.getScore();
            score += diff;

            if (score > 99) {
                score = 100;
                card.setStatus(CardStatus.LEARNT);
            }
            card.setScore(score);
            cards.add(card);
        }
        try {
            connection.open();
            cardDao.batchUpdateScores(cards);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while increasing scores up for cards with id = {}", cardIds, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public Word addToVocabulary(OwnerId ownerId, int vocabId, int wordRef) {

        Word word = new Word();
        word = word.withId(wordRef);
        List<Word> wordsheetItems = wordsCache.get(vocabId);
        wordsheetItems.add(word);

        vocabularyDao.addWordsToVocabulary(vocabId, Set.of(wordRef));

        return word;
    }

    @Override
    public boolean removeFromVocabulary(OwnerId ownerId, int vocabId, int wordRef) {

        List<Word> wordsheetItems = wordsCache.get(vocabId);
        wordsheetItems.removeIf(wordsheetItem -> wordsheetItem.getId() == wordRef);

        vocabularyDao.removeWordsFromVocabulary(vocabId, Set.of(wordRef));

        return true;
    }

    private Vocabulary findVocab(OwnerId ownerId, int dictionaryId) {
        return dictionariesCache.getOrDefault(ownerId, Collections.emptyList()).stream()
                .filter(vocabulary -> vocabulary.getVocabId() == dictionaryId)
                .findAny()
                .orElseGet(() -> loadVocabFromDb(ownerId, dictionaryId));
    }

    private Vocabulary loadVocabFromDb(OwnerId ownerId, int vocabId) {
        Vocabulary vocabulary;
        try {
            vocabulary = vocabularyDao.selectById(vocabId)
                    .orElseThrow(() -> new VocabNotFoundException("Vocabulary with id = " + vocabId + " not found for owner id = " + ownerId));
        } catch (DataAccessException e) {
            throw new DictionaryServiceException();
        }
        putToCache(ownerId, () -> vocabulary);
        return vocabulary;
    }

    private Card findCardById(int cardId) {
        return Optional.ofNullable(cardsCache.get(cardId))
                .orElseGet(() -> loadCardFromDb(cardId));
    }

    private Card loadCardFromDb(int cardId) {
        Card card;
        try {
            connection.open();
            card = cardDao.selectById(cardId);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading a card with id = {}", cardId, e);
            throw new DictionaryServiceException();
        }
        if (card == null) {
            throw new CardNotFoundException();
        }
        return card;
    }

    @Override
    public List<Card> generateCards(OwnerId ownerId, int dictionaryId, Set<String> words) {
        try {
            Vocabulary vocabulary = findVocab(ownerId, dictionaryId);
            int cnt = words.size();
            connection.open();
            Set<Integer> generatedIds = wordDao.generate(words);
            if (generatedIds.size() != cnt) {
                throw new IllegalStateException();
            }
            Set<Integer> cardIds = cardDao.generateEmptyCards(vocabulary.getVocabId(), generatedIds);
            if (cardIds.size() != cnt) {
                throw new IllegalStateException();
            }

            List<Word> wordList = wordDao.selectAll(generatedIds);
            connection.commit();

            Map<Integer, Word> wordsMap = wordList
                    .stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));

            Iterator<Integer> wordIds = generatedIds.iterator();

            List<Card> result = new ArrayList<>();
            for (Integer cardId : cardIds) {
                Integer wordId = wordIds.next();
                Card card = new Card();
                card.setWordId(wordId);
                card.setWord(wordsMap.get(wordId));
                card.setDictionaryId(dictionaryId);
                card.setId(cardId);
                card.setStatus(CardStatus.DEFAULT_STATUS);
                card.setInsertedAt(Instant.now());
                card.setScore(0);
                result.add(card);
            }
            return result;
        } catch (DaoException e) {
            LOG.error("Error while generating cards without definitions, dictionaryId = {}", dictionaryId, e);
            connection.rollback();
            return Collections.emptyList();
        } finally {
            connection.close();
        }
    }

    private void putToCache(OwnerId ownerId, Supplier<Vocabulary> vocabulary) {
        if (dictionariesCache.containsKey(ownerId)) {
            List<Vocabulary> dictionaries = dictionariesCache.get(ownerId);
            dictionaries.add(vocabulary.get());
        } else {
            List<Vocabulary> newList = new ArrayList<>();
            newList.add(vocabulary.get());
            dictionariesCache.put(ownerId, newList);
        }
    }

}