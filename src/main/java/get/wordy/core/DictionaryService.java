package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.*;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.ConnectionWrapper;
import get.wordy.core.wrapper.Score;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DictionaryService implements IDictionaryService {

    private static final Logger LOG = Logger.getLogger(DictionaryService.class.getName());

    private DictionaryDao dictionaryDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private ConnectionWrapper connection;
    private final List<Dictionary> dictionaryList = new ArrayList<>();
    private final Map<Integer, Card> cardsCache = new HashMap<>();

    public DictionaryService() {
    }

    public DictionaryService(DictionaryDao dictionaryDao,
                             WordDao wordDao,
                             CardDao cardDao,
                             ConnectionWrapper connection
    ) {
        this.dictionaryDao = dictionaryDao;
        this.wordDao = wordDao;
        this.cardDao = cardDao;
        this.connection = connection;
    }

    @Override
    public List<Dictionary> getDictionaries() {
        List<Dictionary> list = null;
        try {
            connection.open();
            list = dictionaryDao.selectAll();
            dictionaryList.clear();
            dictionaryList.addAll(list);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading dictionaries", e);
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
            LOG.log(Level.WARNING, "Error while creating a new dictionary", e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
        return dictionary;
    }

    @Override
    public boolean renameDictionary(int dictionaryId, String newDictionaryName) {
        Dictionary dictionary = findDictionary(dictionaryId);
        if (dictionary == null) {
            LOG.log(Level.WARNING, "No dictionary was renamed");
            return false;
        }
        Dictionary copy = new Dictionary(dictionary.getId(), newDictionaryName, null);
        try {
            connection.open();
            dictionaryDao.update(copy);
            connection.commit();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while renaming dictionary", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean removeDictionary(int dictionaryId) {
        Dictionary dictionary = findDictionary(dictionaryId);
        if (dictionary == null) {
            LOG.log(Level.WARNING, "No dictionary was removed");
            return false;
        }
        try {
            connection.open();
            dictionaryDao.delete(dictionary);
            connection.commit();
            dictionaryList.remove(dictionary);
        } catch (DaoException e) {
            connection.rollback();
            LOG.log(Level.WARNING, "Error while removing dictionary", e);
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public Set<Card> getCards(int dictionaryId) {
        List<Card> cardList;
        List<Word> wordList;
        Map<Integer, List<Context>> contextMap = new HashMap<>();
        Map<Integer, List<Collocation>> collocationMap = new HashMap<>();
        try {
            connection.open();
            cardList = cardDao.selectCardsForDictionary(findDictionary(dictionaryId));
            wordList = wordDao.selectAll();
            for (Card card : cardList) {
                int wordId = card.getWordId();
                contextMap.put(wordId, cardDao.getContextsFor(card));
                collocationMap.put(wordId, cardDao.getCollocationsFor(card));
            }
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading card or word beans", e);
            return Collections.emptySet();
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

        return Set.copyOf(cardList);
    }

    @Override
    public List<Card> getCardsForExercise(int dictionaryId, int limit) {
        int[] cardIds;
        try {
            connection.open();
            cardIds = cardDao.selectCardIdsForExercise(dictionaryId, limit);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading exercise set", e);
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
    public boolean addCard(int dictionaryId, Card card) {
        try {
            connection.open();

            Word insertedWord = wordDao.insert(card.getWord());
            int wordId = insertedWord.getId();

            card.setDictionaryId(dictionaryId);
            card.setWordId(wordId);
            card.setInsertedAt(Instant.now());
            card.getContexts().forEach(context -> context.setWordId(wordId));
            card.getCollocations().forEach(collocation -> collocation.setWordId(wordId));
            Card insertedCard = cardDao.insert(card);

            connection.commit();
            int cardId = insertedCard.getId();

            cardsCache.put(cardId, card);

            return wordId > 0 && cardId > 0;
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while saving card", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean updateCard(int dictionaryId, Card card) {
        int cardId = card.getId();
        Card oldCard = findCardById(cardId);

        if (card.equals(oldCard)) {
            return false;
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
            LOG.log(Level.WARNING, "Error while updating card", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean removeCard(int cardId) {
        Card card = findCardById(cardId);
        try {
            connection.open();
            cardDao.delete(card);
            Word word = card.getWord();
            wordDao.delete(word);
            connection.commit();
            cardsCache.remove(cardId);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while removing card", e);
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
            if (card.getContexts().isEmpty()) {
                List<Context> contexts = cardDao.getContextsFor(card);
                List<Collocation> collocations = cardDao.getCollocationsFor(card);
                card.addContexts(contexts);
                card.addCollocations(collocations);
            }
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading card", e);
            return null;
        } catch (NullPointerException npe) {
            return null;
        } finally {
            connection.close();
        }
        return card;
    }

    @Override
    public boolean changeStatus(final int cardId, CardStatus newStatus) {
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
            LOG.log(Level.WARNING, "Error while changing status", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public Score getScore(int dictionaryId) {
        Score score = new Score();
        try {
            connection.open();
            Map<String, Integer> result = cardDao.getScore(findDictionary(dictionaryId));
            result.keySet()
                    .stream()
                    .map(CardStatus::valueOf)
                    .forEach(status -> score.setScoreCount(status, result.get(status.name())));
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while getting score for dictionary", e);
            return null;
        } finally {
            connection.close();
        }
        return score;
    }

    @Override
    public boolean resetScore(int dictionaryId) {
        try {
            connection.open();
            cardDao.resetScore(findDictionary(dictionaryId));
            connection.commit();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while resetting score", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean increaseScoreUp(final int cardId, int repetitions) {
        Card card = findCardById(cardId);

        int diff = 100 / repetitions;
        int score = card.getScore();
        score += diff;

        if (score > 99) {
            score = 100;
            card.setStatus(CardStatus.LEARNT);
        }
        card.setScore(score);

        try {
            connection.open();
            cardDao.update(card);
            connection.commit();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while increasing score up", e);
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
                .findFirst()
                .orElse(null);
    }

    private Card findCardById(int cardId) {
        return cardsCache.get(cardId);
    }

    @Override
    public List<Card> generateCards(int dictionaryId, Set<String> words) {
        int dicId = findDictionary(dictionaryId).getId();
        int cnt = words.size();

        try {
            connection.open();
            Set<Integer> generatedIds = wordDao.generate(words);
            if (generatedIds.size() != cnt) {
                throw new IllegalStateException();
            }
            Set<Integer> cardIds = cardDao.generateEmptyCards(dicId, generatedIds);
            if (cardIds.size() != cnt) {
                throw new IllegalStateException();
            }

            List<Card> result = new ArrayList<>();
            List<Word> wordList = wordDao.selectAll();
            connection.commit();

            Map<Integer, Word> wordsMap = wordList
                    .stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));

            Iterator<Integer> wordIds = generatedIds.iterator();
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
            LOG.log(Level.WARNING, "Error while generating cards without definitions", e);
            connection.rollback();
            return Collections.emptyList();
        } finally {
            connection.close();
        }
    }

}