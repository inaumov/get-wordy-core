package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.ICardDao;
import get.wordy.core.api.dao.IDictionaryDao;
import get.wordy.core.api.dao.IWordDao;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Word;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.ADaoFactory;
import get.wordy.core.db.ConnectionFactory;
import get.wordy.core.bean.Card;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DictionaryService implements IDictionaryService {

    private static final Logger LOG = Logger.getLogger(DictionaryService.class.getName());

    private static final String DEFAULT_URI = "sql/ServerInfo.xml";

    private ConnectionFactory connection = ConnectionFactory.getInstance();
    private ADaoFactory daoFactory;

    private IDictionaryDao dictionaryDao;
    private IWordDao wordDao;
    private ICardDao cardDao;

    private List<Dictionary> dictionaryList = new ArrayList<Dictionary>();
    private Map<String, Card> cardsCache = new HashMap<String, Card>();

    public DictionaryService() {
        this(DEFAULT_URI);
    }

    public DictionaryService(String uri) {
        ServerInfoParser serverInfoParser = new ServerInfoParser();
        serverInfoParser.parseDocument(uri);
        ServerInfo serverInfo = serverInfoParser.getServerInfo();

        initDaoLayer(serverInfo);
    }

    public DictionaryService(ServerInfo customInfo) {
        initDaoLayer(customInfo);
    }

    private void initDaoLayer(ServerInfo serverInfo) {
        if (serverInfo == null) {
            throw new NullPointerException(ServerInfo.class.getSimpleName() + "is not set properly");
        }
        connection.setServerInfo(serverInfo);

        daoFactory = ADaoFactory.getFactory();

        dictionaryDao = daoFactory.getDictionaryDao();
        wordDao = daoFactory.getWordDao();
        cardDao = daoFactory.getCardDao();
    }

    // dictionary section

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
    public boolean createDictionary(String dictionaryName) {
        Dictionary dictionary = new Dictionary();
        dictionary.setName(dictionaryName);
        try {
            connection.open();
            dictionaryDao.insert(dictionary);
            connection.commit();
            dictionaryList.add(dictionary);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while creating name", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean renameDictionary(String oldDictionaryName, String newDictionaryName) {
        Dictionary dictionary = findDictionary(oldDictionaryName);
        if (dictionary == null) {
            LOG.log(Level.WARNING, "No dictionary was renamed");
            return false;
        }
        Dictionary copy = new Dictionary(dictionary.getId(), newDictionaryName);
        try {
            connection.open();
            dictionaryDao.update(copy);
            connection.commit();
            dictionary.setName(newDictionaryName);
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
    public boolean removeDictionary(String dictionaryName) {
        Dictionary dictionary = findDictionary(dictionaryName);
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

    // cards section

    @Override
    public Map<String, Card> getCards(String dictionaryName) {
        List<get.wordy.core.bean.Card> cardList;
        List<Word> wordList;
        try {
            connection.open();
            cardList = cardDao.selectCardsForDictionary(findDictionary(dictionaryName));
            wordList = wordDao.selectAll();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading card or word beans", e);
            return Collections.emptyMap();
        } finally {
            connection.close();
        }

        Comparator<Word> c = new Comparator<Word>() {
            public int compare(Word w1, Word w2) {
                return w1.getId() - w2.getId();
            }
        };

        HashMap<String, Card> cardsMap = new HashMap<String, Card>();

        Iterator<get.wordy.core.bean.Card> cardIterator = cardList.iterator();
        while (cardIterator.hasNext()) {
            get.wordy.core.bean.Card card = cardIterator.next();
            Word word = findWord(card.getWordId(), wordList, c);
            card.setWord(word);
            cardsMap.put(word.getValue(), card);
        }
        cardsCache.putAll(cardsMap);

        return cardsMap;
    }

    @Override
    public Map<String, Card> getCardsForExercise(String dictionaryName, int limit) {
        int[] cardIds;
        try {
            connection.open();
            cardIds = cardDao.selectCardsForExercise(findDictionary(dictionaryName), limit);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading exercise set", e);
            return Collections.emptyMap();
        } finally {
            connection.close();
        }
        Map<String, Card> exercises = new HashMap<String, Card>();
        for (int id : cardIds) {
            Card card = findCardById(id);
            if (card != null) {
                exercises.put(card.getWord().getValue(), card);
            }
        }
        return exercises;
    }

    @Override
    public boolean saveOrUpdateCard(Card card, String dictionaryName) {
        return card.getId() > 0 ? updateCard(card, dictionaryName) : saveCard(card, dictionaryName);
    }

    private boolean saveCard(Card card, String dictionaryName) {
        Word word = card.getWord();
        try {
            connection.open();

            wordDao.insert(word);

            card.setId(word.getId());
            card.setDictionaryId(findDictionary(dictionaryName).getId());
            cardDao.insert(card);

            connection.commit();
            cardsCache.put(word.getValue(), card);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while saving card", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return word.getId() != 0 && card.getId() != 0;
    }

    private boolean updateCard(Card card, String dictionaryName) {
        Card oldCard = findCardById(card.getId());

        Word word = card.getWord();
        try {
            connection.open();

            if (!word.equals(oldCard.getWord())) {
                wordDao.update(word);
            }

            card.setDictionaryId(findDictionary(dictionaryName).getId());
            if (!card.equals(oldCard)) {
                cardDao.update(card);
            }

            connection.commit();
            cardsCache.remove(oldCard.getWord().getValue());
            cardsCache.put(word.getValue(), card);
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
    public boolean removeCard(String wordToRemove) {
        Card card = findCardByWord(wordToRemove);
        try {
            connection.open();
            cardDao.delete(card);
            Word word = card.getWord();
            // todo: in the future remove only a word which is NOT from third-party dictionary
            wordDao.delete(word);
            connection.commit();
            cardsCache.remove(word.getValue());
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
    public Card loadCard(String word) {
        Card card = findCardByWord(word);
        try {
            connection.open();
            List<Definition> definitions = cardDao.getDefinitionsFor(card);
            card.addAll(definitions);
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

    // status section

    @Override
    public boolean changeStatus(final String word, CardStatus updatedStatus) {
        Card card = findCardByWord(word);
        card.setStatus(updatedStatus);

        if (updatedStatus == CardStatus.LEARNT) {
            card.setRating(100);
        } else if (updatedStatus == CardStatus.EDIT) {
            card.setRating(0);
        }

        try {
            connection.open();
            cardDao.update(card);
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

    // score section

    @Override
    public IScore getScore(String dictionaryName) {
        IScore score = null;
        try {
            connection.open();
            score = cardDao.getScore(findDictionary(dictionaryName));
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while getting score for dictionary", e);
            return null;
        } finally {
            connection.close();
        }
        return score;
    }

    @Override
    public boolean resetScore(String dictionaryName) {
        try {
            connection.open();
            cardDao.resetScore(findDictionary(dictionaryName));
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
    public boolean increaseScoreUp(final String word, int repetitions) {
        Card card = findCardByWord(word);

        int diff = 100 / repetitions;
        int rating = card.getRating();
        rating += diff;

        if (rating > 99) {
            rating = 100;
            card.setStatus(CardStatus.LEARNT);
        }
        card.setRating(rating);

        try {
            connection.open();
            cardDao.update(card);
            connection.commit();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while increasing rating up", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    private Dictionary findDictionary(String dictionaryName) {
        Iterator<Dictionary> dictionaryIterator = dictionaryList.iterator();
        while (dictionaryIterator.hasNext()) {
            Dictionary dictionary = dictionaryIterator.next();
            String name = dictionary.getName();
            if (dictionaryName.equals(name)) {
                return dictionary;
            }
        }
        return null;
    }

    private Word findWord(int id, List<Word> wordList, Comparator<Word> c) {
        int index = Collections.binarySearch(wordList, new Word(id, null), c);
        return wordList.get(index);
    }

    private Card findCardByWord(String word) {
        return cardsCache.get(word);
    }

    private Card findCardById(int cardId) {
        Iterator<Card> it = cardsCache.values().iterator();
        while (it.hasNext()) {
            Card card = it.next();
            if (card.getId() == cardId) {
                return card;
            }
        }
        return null;
    }

}