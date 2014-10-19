package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.*;
import get.wordy.core.bean.*;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.ADaoFactory;
import get.wordy.core.db.ConnectionFactory;
import get.wordy.core.wrapper.CardItem;
import get.wordy.core.wrapper.DefinitionPair;

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
    private Map<String, CardItem> cardItemsCache = new HashMap<String, CardItem>();

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
    public Map<String, CardItem> getCards(String dictionaryName) {
        List<Card> cardList;
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

        HashMap<String, CardItem> cardItemMap = new HashMap<String, CardItem>();

        Iterator<Card> cardIterator = cardList.iterator();
        while (cardIterator.hasNext()) {
            Card card = cardIterator.next();
            Word word = findWord(card.getWordId(), wordList, c);
            CardItem cardItem = new CardItem(card, word);
            cardItemMap.put(word.getValue(), cardItem);
        }
        cardItemsCache.putAll(cardItemMap);

        return cardItemMap;
    }

    @Override
    public Map<String, CardItem> getCardsForExercise(String dictionaryName, int limit) {
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
        Map<String, CardItem> exercises = new HashMap<String, CardItem>();
        for (int id : cardIds) {
            CardItem cardItem = findCardItemById(id);
            if (cardItem != null) {
                exercises.put(cardItem.getWord().getValue(), cardItem);
            }
        }
        return exercises;
    }

    @Override
    public boolean saveOrUpdateCard(CardItem cardItem) {
        return cardItem.getId() > 0 ? updateCard(cardItem) : saveCard(cardItem);
    }

    private boolean saveCard(CardItem cardItem) {
        Word word = cardItem.getWord();
        Card card = cardItem.getCard();
        try {
            connection.open();
            wordDao.insert(word);
            cardDao.insert(card);
            connection.commit();
            cardItemsCache.put(word.getValue(), cardItem);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while saving card", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return word.getId() != 0 && card.getId() != 0;
    }

    private boolean updateCard(CardItem cardItem) {
        CardItem oldCardItem = findCardItemById(cardItem.getId());

        Word word = cardItem.getWord();
        Card card = cardItem.getCard();
        try {
            connection.open();
            if (!word.equals(oldCardItem.getWord())) {
                wordDao.update(word);
            }
            if (!card.equals(oldCardItem.getCard())) {
                cardDao.update(card);
            }

            connection.commit();
            cardItemsCache.remove(oldCardItem.getWord().getValue());
            cardItemsCache.put(word.getValue(), cardItem);
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
        CardItem cardItem = findCardItemByWord(wordToRemove);
        try {
            connection.open();
            cardDao.delete(cardItem.getCard());
            Word word = cardItem.getWord();
            // todo: in the future remove only a word which is NOT from third-party dictionary
            wordDao.delete(word);
            connection.commit();
            cardItemsCache.remove(word.getValue());
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
    public CardItem loadCard(String word) {
        CardItem cardItem = findCardItemByWord(word);
        try {
            connection.open();
            Collection<Definition> definitions = cardDao.getDefinitionsFor(cardItem.getCard());
            Iterator<Definition> it = definitions.iterator();
            while (it.hasNext()) {
                Definition definition = it.next();
                DefinitionPair pair = new DefinitionPair(definition, definition.getMeanings().toArray());
                cardItem.addDefinitionPair(definition.getGramUnit(), pair);
            }
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Error while loading card", e);
            return null;
        } catch (NullPointerException npe) {
            return null;
        } finally {
            connection.close();
        }
        return cardItem;
    }

    // status section

    @Override
    public boolean changeStatus(final String word, CardStatus updatedStatus) {
        CardItem cardItem = findCardItemByWord(word);
        Card card = cardItem.getCard();
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
        CardItem cardItem = findCardItemByWord(word);

        Card card = cardItem.getCard();

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

    private CardItem findCardItemByWord(String word) {
        return cardItemsCache.get(word);
    }

    private CardItem findCardItemById(int cardId) {
        Iterator<CardItem> it = cardItemsCache.values().iterator();
        while (it.hasNext()) {
            CardItem cardItem = it.next();
            if (cardItem.getId() == cardId) {
                return cardItem;
            }
        }
        return null;
    }

}