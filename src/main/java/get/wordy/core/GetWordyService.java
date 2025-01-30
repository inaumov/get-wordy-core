package get.wordy.core;

import get.wordy.core.api.IUserCardsService;
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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetWordyService implements IUserCardsService, IVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(GetWordyService.class);

    private VocabularyDao vocabularyDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private CardHeadlineDao cardHeadlineDao;
    private LocalTxManager connection;
    private final Map<OwnerId, List<Vocabulary>> userVocabsCache = new HashMap<>();
    private final Map<Integer, Card> cardsCache = new HashMap<>();
    private final Map<Integer, List<Word>> wordsInVocabularyCache = new HashMap<>();

    @SuppressWarnings("unused")
    public GetWordyService() {
    }

    @SuppressWarnings("unused")
    public GetWordyService(VocabularyDao vocabularyDao,
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

        List<Vocabulary> vocabularies = userVocabsCache.get(ownerId);
        if (vocabularies != null && !vocabularies.isEmpty()) {
            return vocabularies;
        }

        List<Vocabulary> list;
        try {
            // fetch from the database if not present in the cache
            list = vocabularyDao.selectAllByOwnerId(ownerId);

            // update the cache
            userVocabsCache.put(ownerId, list);
        } catch (DataAccessException e) {
            LOG.error("Error while loading vocabularies", e);
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
    public boolean hasVocabulary(OwnerId ownerId, int vocabId) throws VocabNotFoundException {
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        return vocabulary != null;
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
            userVocabsCache.get(ownerId)
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
        if (wordsInVocabularyCache.containsKey(vocabId)) {
            List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
            if (!CollectionUtils.isEmpty(wordsInVocab)) {
                return wordsInVocab;
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

        wordsInVocabularyCache.put(vocabId, vocabulary);

        return List.copyOf(vocabulary);
    }

    @Override
    public List<Card> getCards(OwnerId ownerId, int vocabId) {
        List<Card> cardListFull;
        try {
            connection.open();
            cardListFull = cardHeadlineDao.getCards(findVocab(ownerId, vocabId).getVocabId());
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all cards in vocabulary by id = {}", vocabId, e);
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
    public List<Exercise> getCardsForExercise(OwnerId ownerId, int vocabId, int limit) {
        List<Exercise> exercises = new ArrayList<>();
        try {
            connection.open();
            int[] cardIds = cardDao.selectCardIdsForExercise(ownerId, vocabId, limit);
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
            LOG.error("Error while loading cards for exercise by vocab id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return exercises;
    }

    @Override
    public Card addCard(OwnerId ownerId, int vocabId, int wordId) {
        Card card = new Card();
        card.setVocabId(vocabId);
        card.setWordId(wordId);
        card.setStatus(CardStatus.TO_LEARN);

        try {
            connection.open();

            Card insertedCard = cardDao.insert(ownerId, card);

            connection.commit();
            int cardId = insertedCard.getId();
            if (cardId > 0) {
                cardsCache.put(cardId, insertedCard);
                return insertedCard;
            } else {
                throw new DictionaryServiceException();
            }
        } catch (DaoException e) {
            LOG.error("Error while saving a new card", e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean deleteCard(OwnerId ownerId, int cardId) {
        Card card = findCardById(cardId);
        try {
            connection.open();
            cardDao.delete(ownerId, card.getId());
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
    public Score getScoreSummary(OwnerId ownerId, int vocabId) {
        try {
            Vocabulary vocabulary = findVocab(ownerId, vocabId);
            Score score = new Score();
            connection.open();
            Map<String, Integer> result = cardDao.getScoreSummary(ownerId, vocabulary.getVocabId());
            connection.commit();
            Set<String> statuses = result.keySet();
            for (String status : statuses) {
                score.withScoreCount(CardStatus.valueOf(status), result.get(status));
            }
            return score;
        } catch (DaoException e) {
            LOG.error("Error while getting total score summary for vocabulary, id = {}", vocabId, e);
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean resetScore(OwnerId ownerId, int cardId) {
        Card card = findCardById(cardId);
        LOG.info("Resetting score for a card id = {}", cardId);
        try {
            connection.open();
            cardDao.updateStatus(card.getId(), CardStatus.TO_LEARN);
            cardDao.updateScore(card.getId(), 0);
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
    public boolean increaseScoreUp(OwnerId ownerId, int vocabId, int[] cardIds, int repetitions) {
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        // omit duplicates if any
        int[] uniqueCardIds = Arrays.stream(cardIds)
                .distinct()
                .toArray();
        List<Card> cards = new ArrayList<>();
        List<Card> onlyLearnt = new ArrayList<>();
        for (int cardId : uniqueCardIds) {
            Card card = findCardById(cardId);
            // todo get all cards by vocabId -> filter and iterate over it
            int diff = 100 / repetitions;
            int score = card.getScore();
            score += diff;

            if (score > 99) {
                score = 100;
                card.setStatus(CardStatus.LEARNT);
                onlyLearnt.add(card);
            }
            card.setScore(score);
            cards.add(card);
        }
        try {
            connection.open();
            cardDao.batchUpdateScores(cards);
            cardDao.batchUpdateStatuses(onlyLearnt);
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
        List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
        wordsInVocab.add(word);

        vocabularyDao.addWordsToVocabulary(vocabId, Set.of(wordRef));

        return word;
    }

    @Override
    public boolean removeFromVocabulary(OwnerId ownerId, int vocabId, int wordRef) {

        List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
        wordsInVocab.removeIf(wordEntity -> wordEntity.getId() == wordRef);

        vocabularyDao.removeWordsFromVocabulary(vocabId, Set.of(wordRef));

        return true;
    }

    private Vocabulary findVocab(OwnerId ownerId, int vocabId) {
        return userVocabsCache.getOrDefault(ownerId, Collections.emptyList()).stream()
                .filter(vocabulary -> vocabulary.getVocabId() == vocabId)
                .findAny()
                .orElseGet(() -> loadVocabFromDb(ownerId, vocabId));
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
            card = cardDao.selectById(cardId);
        } catch (DaoException e) {
            LOG.error("Error while loading a card by id = {}", cardId, e);
            throw new DictionaryServiceException();
        }
        if (card == null) {
            throw new CardNotFoundException();
        }
        return card;
    }

    @Override
    public List<Card> generateCards(OwnerId ownerId, int vocabId, Set<Integer> wordRefs) {
        Vocabulary vocabulary = findVocab(ownerId, vocabId);

        try {
            connection.open();
            cardDao.addCards(ownerId, vocabulary.getVocabId(), wordRefs);
            connection.commit();

            List<Word> wordList = wordDao.selectAll(wordRefs);

            Map<Integer, Word> wordsMap = wordList
                    .stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            // refresh
            List<Card> result = cardDao.selectCards(ownerId, vocabId);
            for (Card card : result) {
                card.setWord(wordsMap.get(card.getWordId()));
            }
            return result;
        } catch (DaoException e) {
            LOG.error("Error while generating cards by vocabId = {}", vocabId, e);
            connection.rollback();
            return Collections.emptyList();
        } finally {
            connection.close();
        }
    }

    private void putToCache(OwnerId ownerId, Supplier<Vocabulary> vocabulary) {
        if (userVocabsCache.containsKey(ownerId)) {
            List<Vocabulary> vocabularies = userVocabsCache.get(ownerId);
            vocabularies.add(vocabulary.get());
        } else {
            List<Vocabulary> newList = new ArrayList<>();
            newList.add(vocabulary.get());
            userVocabsCache.put(ownerId, newList);
        }
    }

}