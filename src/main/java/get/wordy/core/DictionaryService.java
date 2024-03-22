package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.exception.CardNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.api.bean.wrapper.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DictionaryService implements IDictionaryService {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryService.class);

    private DictionaryDao dictionaryDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private LocalTxManager connection;
    private final List<Dictionary> dictionaryList = new ArrayList<>();
    private final Map<Integer, Card> cardsCache = new HashMap<>();

    @SuppressWarnings("unused")
    public DictionaryService() {
    }

    @SuppressWarnings("unused")
    public DictionaryService(DictionaryDao dictionaryDao,
                             WordDao wordDao,
                             CardDao cardDao,
                             LocalTxManager connection
    ) {
        this.dictionaryDao = dictionaryDao;
        this.wordDao = wordDao;
        this.cardDao = cardDao;
        this.connection = connection;
    }

    @Override
    public List<Dictionary> getDictionaries() {
        List<Dictionary> list;
        try {
            connection.open();
            list = dictionaryDao.selectAll();
            connection.commit();
            dictionaryList.clear();
            dictionaryList.addAll(list);
        } catch (DaoException e) {
            LOG.error("Error while loading dictionaries", e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return list;
    }

    @Override
    public Dictionary createDictionary(String dictionaryName, String picture) {
        Dictionary dictionary = new Dictionary();
        dictionary.setName(dictionaryName);
        dictionary.setPicture(picture);
        try {
            connection.open();
            dictionaryDao.insert(dictionary);
            connection.commit();
            dictionaryList.add(dictionary);
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
    public boolean renameDictionary(int dictionaryId, String newDictionaryName) {
        try {
            Dictionary dictionary = findDictionary(dictionaryId);
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
    public boolean changeDictionaryPicture(int dictionaryId, String newPictureUrl) {
        try {
            Dictionary dictionary = findDictionary(dictionaryId);
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
    public boolean deleteDictionary(int dictionaryId) {
        try {
            Dictionary dictionary = findDictionary(dictionaryId);
            connection.open();
            dictionaryDao.delete(dictionary);
            connection.commit();
            dictionaryList.remove(dictionary);
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
    public List<Card> getCards(int dictionaryId) {
        List<Card> cardList;
        List<Word> wordList;
        Map<Integer, List<Context>> contextMap = new HashMap<>();
        Map<Integer, List<Collocation>> collocationMap = new HashMap<>();
        try {
            connection.open();
            // todo: optimize
            cardList = cardDao.selectCardsForDictionary(findDictionary(dictionaryId));
            wordList = wordDao.selectAll();
            for (Card card : cardList) {
                int wordId = card.getWordId();
                contextMap.put(wordId, cardDao.getContextsFor(card));
                collocationMap.put(wordId, cardDao.getCollocationsFor(card));
            }
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all cards in dictionary by id = {}", dictionaryId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }

        Map<Integer, Word> wordsMap = wordList
                .stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));

        HashMap<Integer, Card> cardsMap = new HashMap<>();

        for (Card card : cardList) {
            int wordId = card.getWordId();
            card.setWord(wordsMap.get(wordId));
            card.addContexts(contextMap.get(wordId));
            card.addCollocations(collocationMap.get(wordId));
            cardsMap.put(card.getId(), card);
        }

        cardsCache.clear();
        cardsCache.putAll(cardsMap);

        return List.copyOf(cardList);
    }

    @Override
    public List<Card> getCardsForExercise(int dictionaryId, int limit) {
        int[] cardIds;
        try {
            connection.open();
            cardIds = cardDao.selectCardIdsForExercise(dictionaryId, limit);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading exercise cards set for dictionary, id = {}", dictionaryId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        List<Card> exercises = new ArrayList<>();
        for (int id : cardIds) {
            Card card = findCardById(id);
            if (card != null) {
                exercises.add(card);
            }
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
            card.setInsertedAt(Instant.now());
            Card insertedCard = cardDao.insert(card);

            connection.commit();
            int cardId = insertedCard.getId();
            if (cardId > 0) {
                cardsCache.put(cardId, card);
                return insertedCard;
            } else {
                throw new DictionaryServiceException();
            }
        } catch (DaoException e) {
            LOG.error("Error while saving a card into dictionary",  e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
    }

    @Override
    public Card updateCard(int dictionaryId, Card card) {
        int cardId = card.getId();
        Card oldCard = findCardById(cardId);

        if (card.equals(oldCard)) {
            return card;
        }

        try {
            connection.open();

            Word word = card.getWord();
            if (!word.equals(oldCard.getWord())) {
                wordDao.update(word);
            }
            cardDao.update(card);
            connection.commit();

            cardsCache.put(cardId, card);

        } catch (DaoException e) {
            LOG.error("Error while updating card, id = {}", cardId, e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
        return card;
    }

    @Override
    public boolean deleteCard(int cardId) {
        Card card = findCardById(cardId);
        try {
            connection.open();
            cardDao.delete(card);
            Word word = card.getWord();
            wordDao.delete(word);
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
        Card card = findCardById(cardId);
        try {
            connection.open();
            // todo: optimize
            if (card.getWord() == null) {
                Word word = wordDao.selectById(card.getWordId());
                card.setWord(word);
            }
            if (card.getContexts().isEmpty()) {
                List<Context> contexts = cardDao.getContextsFor(card);
                card.addContexts(contexts);
            }
            if (card.getCollocations().isEmpty()) {
                List<Collocation> collocations = cardDao.getCollocationsFor(card);
                card.addCollocations(collocations);
            }
            connection.commit();
            return card;
        } catch (DaoException e) {
            LOG.error("Error while loading card with id = {}", cardId, e);
            return null;
        } finally {
            connection.close();
        }
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
    public Score getScoreSummary(int dictionaryId) {
        try {
            Dictionary dictionary = findDictionary(dictionaryId);
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
        List<Card> cards = new ArrayList<>();
        for (int cardId : cardIds) {
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

    private Dictionary findDictionary(int dictionaryId) {
        return dictionaryList.stream()
                .filter(dictionary -> dictionary.getId() == dictionaryId)
                .findAny()
                .orElseGet(() -> getDictionaryFromDb(dictionaryId));
    }

    private Dictionary getDictionaryFromDb(int dictionaryId) {
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
        dictionaryList.add(dictionary);
        return dictionary;
    }

    private Card findCardById(int cardId) {
        return Optional.ofNullable(cardsCache.get(cardId))
                .orElseGet(() -> loadCardHeadlineFromDb(cardId));
    }

    private Card loadCardHeadlineFromDb(int cardId) {
        Card card;
        try {
            connection.open();
            card = cardDao.selectById(cardId);
            connection.commit();
        } catch (DaoException e) {
            throw new DictionaryServiceException();
        }
        if (card == null) {
            throw new CardNotFoundException();
        }
        cardsCache.put(cardId, card);
        return card;
    }

    @Override
    public List<Card> generateCards(int dictionaryId, Set<String> words) {
        try {
            Dictionary dictionary = findDictionary(dictionaryId);
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

            List<Word> wordList = wordDao.selectAll();
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

}