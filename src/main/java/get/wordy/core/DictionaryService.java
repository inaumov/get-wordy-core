package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.exception.CardNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.api.bean.wrapper.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DictionaryService implements IDictionaryService {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryService.class);

    private DictionaryDao dictionaryDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private CardHeadlineDao cardHeadlineDao;
    private LocalTxManager connection;
    private final Map<OwnerId, List<Dictionary>> dictionariesCache = new HashMap<>();
    private final Map<Integer, Card> cardsCache = new HashMap<>();

    @SuppressWarnings("unused")
    public DictionaryService() {
    }

    @SuppressWarnings("unused")
    public DictionaryService(DictionaryDao dictionaryDao,
                             WordDao wordDao,
                             CardDao cardDao,
                             CardHeadlineDao cardHeadlineDao,
                             LocalTxManager connection
    ) {
        this.dictionaryDao = dictionaryDao;
        this.wordDao = wordDao;
        this.cardDao = cardDao;
        this.cardHeadlineDao = cardHeadlineDao;
        this.connection = connection;
    }

    @Override
    public List<Dictionary> getDictionaries(OwnerId ownerId) {

        List<Dictionary> cachedDictionaries = dictionariesCache.get(ownerId);
        if (cachedDictionaries != null && !cachedDictionaries.isEmpty()) {
            return cachedDictionaries;
        }

        List<Dictionary> list;
        try {
            connection.open();
            // fetch from the database if not present in the cache
            list = dictionaryDao.selectAllByOwnerId(ownerId);
            connection.commit();

            // update the cache
            dictionariesCache.put(ownerId, list);
        } catch (DaoException e) {
            LOG.error("Error while loading dictionaries", e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return list;
    }

    @Override
    public Dictionary createDictionary(OwnerId ownerId, String dictionaryName, String picture) {
        Dictionary dictionary = new Dictionary();
        dictionary.setName(dictionaryName);
        dictionary.setPicture(picture);
        try {
            connection.open();
            dictionaryDao.insert(dictionary);
            connection.commit();
            putToCache(ownerId, () -> dictionary);
        } catch (DaoException e) {
            LOG.error("Error while creating a new dictionary", e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
        return dictionary;
    }

    @Override
    public boolean renameDictionary(OwnerId ownerId, int dictionaryId, String newDictionaryName) {
        try {
            Dictionary dictionary = findDictionary(ownerId, dictionaryId);
            Dictionary copy = new Dictionary(dictionaryId, newDictionaryName, null);
            connection.open();
            dictionaryDao.update(copy);
            connection.commit();
            dictionary.setName(newDictionaryName);
        } catch (DaoException e) {
            LOG.error("Error while renaming dictionary, id = {}", dictionaryId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean changeDictionaryPicture(OwnerId ownerId, int dictionaryId, String newPictureUrl) {
        try {
            Dictionary dictionary = findDictionary(ownerId, dictionaryId);
            Dictionary copy = new Dictionary(dictionaryId, null, newPictureUrl);
            connection.open();
            dictionaryDao.update(copy);
            connection.commit();
            dictionary.setPicture(newPictureUrl);
        } catch (DaoException e) {
            LOG.error("Error while changing dictionary picture, id = {}", dictionaryId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean deleteDictionary(OwnerId ownerId, int dictionaryId) {
        try {
            Dictionary dictionary = findDictionary(ownerId, dictionaryId);
            connection.open();
            if (dictionary.getCardsTotal() > 0) {
                throw new DictionaryServiceException("Cannot delete dictionary with cards");
            }
            dictionaryDao.delete(dictionaryId);
            connection.commit();
            dictionariesCache.get(ownerId).remove(dictionary);
        } catch (DaoException e) {
            connection.rollback();
            LOG.error("Error while removing dictionary by id = {}", dictionaryId, e);
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public List<Card> getCards(OwnerId ownerId, int dictionaryId) {
        List<Card> cardListFull;
        try {
            connection.open();
            cardListFull = cardHeadlineDao.getCardsForDictionary(findDictionary(ownerId, dictionaryId).getId());
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
        findDictionary(ownerId, dictionaryId);
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
            Dictionary dictionary = findDictionary(ownerId, dictionaryId);
            Score score = new Score();
            connection.open();
            Map<String, Integer> result = cardDao.getScoreSummary(dictionary.getId());
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

    private Dictionary findDictionary(OwnerId ownerId, int dictionaryId) {
        return dictionariesCache.getOrDefault(ownerId, Collections.emptyList()).stream()
                .filter(dictionary -> dictionary.getId() == dictionaryId)
                .findAny()
                .orElseGet(() -> getDictionaryFromDb(ownerId, dictionaryId));
    }

    private Dictionary getDictionaryFromDb(OwnerId ownerId, int dictionaryId) {
        Dictionary dictionary;
        try {
            connection.open();
            dictionary = dictionaryDao.selectById(dictionaryId);
            connection.commit();
        } catch (DaoException e) {
            throw new DictionaryServiceException();
        }
        if (dictionary == null) {
            throw new DictionaryNotFoundException();
        }
        putToCache(ownerId, () -> dictionary);
        return dictionary;
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
            Dictionary dictionary = findDictionary(ownerId, dictionaryId);
            int cnt = words.size();
            connection.open();
            Set<Integer> generatedIds = wordDao.generate(words);
            if (generatedIds.size() != cnt) {
                throw new IllegalStateException();
            }
            Set<Integer> cardIds = cardDao.generateEmptyCards(dictionary.getId(), generatedIds);
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

    private void putToCache(OwnerId ownerId, Supplier<Dictionary> dictionary) {
        if (dictionariesCache.containsKey(ownerId)) {
            List<Dictionary> dictionaries = dictionariesCache.get(ownerId);
            dictionaries.add(dictionary.get());
        } else {
            List<Dictionary> newList = new ArrayList<>();
            newList.add(dictionary.get());
            dictionariesCache.put(ownerId, newList);
        }
    }

}