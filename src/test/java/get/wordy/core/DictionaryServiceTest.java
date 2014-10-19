package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.ICardDao;
import get.wordy.core.api.dao.IDictionaryDao;
import get.wordy.core.api.dao.IWordDao;
import get.wordy.core.bean.Card;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Word;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.db.ConnectionFactory;
import get.wordy.core.wrapper.CardItem;
import junit.framework.TestCase;
import org.easymock.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static org.easymock.EasyMock.*;

@RunWith(EasyMockRunner.class)
public class DictionaryServiceTest extends TestCase {

    private static final String DICTIONARY_NAME = "Test name";

    @Mock
    private ConnectionFactory connection;

    @Mock(name = "dictionaryDao")
    private IDictionaryDao dictionaryDaoMock;
    @Mock(name = "cardDao")
    private ICardDao cardDaoMock;
    @Mock(name = "wordDao")
    private IWordDao wordDaoMock;

    @Mock(name = "dictionaryList", type = MockType.NICE)
    private ArrayList<Dictionary> dictionaryListMock;

    @Mock(name = "cardItemsCache", type = MockType.NICE)
    private HashMap<String, CardItem> cardItemsCacheMock;

    @TestSubject
    private IDictionaryService dictionaryService = new DictionaryService(EasyMock.createMock(ServerInfo.class));

    private Dictionary dictionaryMock;
    private Iterator dictionaryIteratorMock;

    @Before
    public void setUp() throws Exception {
        dictionaryMock = createMock(Dictionary.class);
        expect(dictionaryMock.getName()).andReturn(DICTIONARY_NAME).once();
        expect(dictionaryMock.getId()).andReturn(43).once();

        dictionaryIteratorMock = createMock(Iterator.class);
        expect(dictionaryIteratorMock.hasNext()).andReturn(true).once();
        expect(dictionaryIteratorMock.next()).andReturn(dictionaryMock).once();
    }

    // test dictionary section

    @Test
    public void testGetDictionaries() throws DaoException {
        connection.open();
        expectLastCall().once();

        List<Dictionary> dictionaries = Collections.singletonList(dictionaryMock);
        expect(dictionaryDaoMock.selectAll()).andReturn(dictionaries);
        dictionaryListMock.clear();
        expectLastCall().once();
        expect(dictionaryListMock.addAll(dictionaries)).andReturn(true).once();

        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertEquals(1, list.size());
        verify(dictionaryDaoMock);
    }

    @Test
    public void testGetDictionariesWhenDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryDaoMock.selectAll()).andStubThrow(new DaoException("selectAll", null));

        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertTrue(list.isEmpty());
        verify(dictionaryDaoMock);
    }

    @Test
    public void testCreateDictionary() throws DaoException {
        connection.open();
        expectLastCall().once();

        Capture<Dictionary> dictionaryCapture = new Capture<Dictionary>();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().once();
        expect(dictionaryListMock.add(anyObject(Dictionary.class))).andReturn(true).once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryDaoMock, dictionaryListMock);

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME);
        assertTrue(done);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());
        verify(dictionaryDaoMock, dictionaryListMock);
    }

    @Test
    public void testCreateDictionaryWhenDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        Capture<Dictionary> dictionaryCapture = new Capture<Dictionary>();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("insert", null));

        connection.rollback();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryDaoMock);

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME);
        assertFalse(done);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());
        verify(dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionary() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        Capture<Dictionary> dictionaryCapture = new Capture<Dictionary>();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().once();
        dictionaryMock.setName("nameUpdated");
        expectLastCall().once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_NAME, "nameUpdated");
        assertTrue(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());
        verify(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);
    }

    @Test
    public void testRenameDictionaryWhenDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        Capture<Dictionary> dictionaryCapture = new Capture<Dictionary>();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("rename", null));

        connection.rollback();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_NAME, "nameUpdated");
        assertFalse(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());
        verify(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);
    }

    @Test
    public void testRemoveDictionary() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);

        boolean done = dictionaryService.removeDictionary(DICTIONARY_NAME);
        assertTrue(done);
        verify(dictionaryDaoMock);
    }

    @Test
    public void testRemoveDictionaryWhenDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().andStubThrow(new DaoException("insert", null));

        connection.rollback();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryMock, dictionaryDaoMock, dictionaryListMock, dictionaryIteratorMock);

        boolean done = dictionaryService.removeDictionary(DICTIONARY_NAME);
        assertFalse(done);
        verify(connection, dictionaryDaoMock);
    }

    // test cards section

    @Test
    public void testGetCards() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();
        replay(dictionaryMock, dictionaryListMock);

        Card card = createCardStub(1);
        expect(cardDaoMock.selectCardsForDictionary(anyObject(Dictionary.class))).andReturn(Collections.singletonList(card)).once();

        Word word = createWordStub("word");
        expect(wordDaoMock.selectAll()).andReturn(Collections.singletonList(word)).once();

        cardItemsCacheMock.putAll(anyObject(HashMap.class));
        expectLastCall().once();
        replay(cardDaoMock, wordDaoMock, cardItemsCacheMock);

        connection.close();
        expectLastCall().once();
        replay(connection);

        Map<String, CardItem> cardItems = dictionaryService.getCards(DICTIONARY_NAME);
        assertNotNull(cardItems);
        assertFalse(cardItems.isEmpty());
        verify(wordDaoMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testGetCardsForExercise() throws DaoException {
        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        int[] ids = {1, 2, 3, 5, 8, 13};
        expect(cardDaoMock.selectCardsForExercise(anyObject(Dictionary.class), anyInt())).andReturn(ids).once();

        Collection list = new ArrayList();
        for (int id : ids) {
            list.add(new CardItem(createCardStub(id), createWordStub("word" + id)));
        }

        expect(cardItemsCacheMock.values()).andReturn(list).times(6);
        replay(cardItemsCacheMock);

        replay(dictionaryMock, dictionaryListMock, dictionaryIteratorMock, cardDaoMock);

        Map<String, CardItem> cards = dictionaryService.getCardsForExercise(DICTIONARY_NAME, 6);
        assertEquals(6, cards.size());
    }

    @Test
    public void testSaveCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem cardItemMock = createNiceMock(CardItem.class);
        expect(cardItemsCacheMock.put("word", cardItemMock)).andStubReturn(cardItemMock);
        replay(cardItemsCacheMock);

        Card cardMock = createNiceMock(Card.class);
        Word wordMock = createNiceMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");

        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        expect(cardItemMock.getWord()).andStubReturn(wordMock);
        replay(cardItemMock, cardMock, wordMock);

        wordDaoMock.insert(wordMock);
        expectLastCall().once();
        cardDaoMock.insert(cardMock);
        expectLastCall().once();
        replay(wordDaoMock, cardDaoMock);

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        resetToNice(cardMock, wordMock);
        expect(wordMock.getId()).andReturn(1);
        expect(cardMock.getId()).andReturn(1);
        replay(wordMock, cardMock);

        boolean done = dictionaryService.saveOrUpdateCard(cardItemMock);
        assertTrue(done);
        verify(cardItemMock, cardMock, wordMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testUpdateCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem oldCardItemMock = createNiceMock(CardItem.class);
        Card oldCardMock = createNiceMock(Card.class);
        Word oldWordMock = createNiceMock(Word.class);

        expect(oldCardItemMock.getCard()).andReturn(oldCardMock).once();
        expect(oldCardItemMock.getWord()).andReturn(oldWordMock).times(2);
        expect(oldWordMock.getValue()).andReturn("word");
        replay(oldCardItemMock, oldCardMock, oldWordMock);

        Collection list = Collections.singletonList(oldCardItemMock);
        expect(cardItemsCacheMock.values()).andReturn(list).once();

        CardItem cardItemMock = createNiceMock(CardItem.class);
        expect(cardItemMock.getId()).andReturn(1);
        Card cardMock = createNiceMock(Card.class);
        Word wordMock = createNiceMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");
        expect(cardItemMock.getCard()).andReturn(cardMock).once();
        expect(cardItemMock.getWord()).andReturn(wordMock).once();
        replay(cardItemMock, cardMock, wordMock);

        expect(cardItemsCacheMock.remove("word")).andStubReturn(oldCardItemMock);
        expect(cardItemsCacheMock.put("word", cardItemMock)).andStubReturn(cardItemMock);
        replay(cardItemsCacheMock);

        wordDaoMock.update(wordMock);
        expectLastCall().once();
        cardDaoMock.update(cardMock);
        expectLastCall().once();
        replay(wordDaoMock, cardDaoMock);

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        boolean done = dictionaryService.saveOrUpdateCard(cardItemMock);

        assertTrue(done);
        verify(cardItemMock, cardMock, wordMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testRemoveCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem cardItemMock = createMock(CardItem.class);
        expect(cardItemsCacheMock.get(anyString())).andStubReturn(cardItemMock);
        expect(cardItemsCacheMock.remove(anyString())).andStubReturn(cardItemMock);
        replay(cardItemsCacheMock);

        Card cardMock = createMock(Card.class);
        Word wordMock = createMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");
        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        expect(cardItemMock.getWord()).andStubReturn(wordMock);
        replay(cardItemMock, cardMock, wordMock);

        cardDaoMock.delete(cardMock);
        expectLastCall().once();
        replay(cardDaoMock);

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);


        boolean done = dictionaryService.removeCard("word");
        assertTrue(done);
        verify(cardItemMock, cardMock, wordMock);
        verify(cardDaoMock);
        verify(connection);
    }


    // test status section

    @Test
    public void testChangeStatusWhenThrowDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem cardItemMock = createMock(CardItem.class);
        expect(cardItemsCacheMock.get(anyString())).andStubReturn(cardItemMock);
        replay(cardItemsCacheMock);

        Card cardMock = createMock(Card.class);
        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        replay(cardItemMock);

        cardMock.setStatus(CardStatus.DEFAULT_STATUS);
        cardMock.setRating(0);
        replay(cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().andStubThrow(new DaoException("changeStatus", null));
        replay(cardDaoMock);

        connection.rollback();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        boolean done = dictionaryService.changeStatus("word", CardStatus.DEFAULT_STATUS);
        assertFalse(done);
        verify(cardDaoMock);
        verify(connection);
    }

    @Test
    public void testChangeStatus() throws DaoException {
        CardItem cardItemMock = createMock(CardItem.class);
        expect(cardItemsCacheMock.get(anyString())).andStubReturn(cardItemMock);
        replay(cardItemsCacheMock);

        Card cardMock = createMock(Card.class);
        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        replay(cardItemMock);

        cardMock.setStatus(CardStatus.EDIT);
        cardMock.setRating(0);
        replay(cardMock);
        doChangeStatusTest(cardMock, CardStatus.EDIT);

        reset(cardMock);
        reset(connection, cardDaoMock);
        cardMock.setStatus(CardStatus.TO_LEARN);
        replay(cardMock);
        doChangeStatusTest(cardMock, CardStatus.TO_LEARN);

        reset(cardMock);
        reset(connection, cardDaoMock);
        cardMock.setStatus(CardStatus.LEARNT);
        cardMock.setRating(100);
        replay(cardMock);
        doChangeStatusTest(cardMock, CardStatus.LEARNT);

        reset(cardMock);
        reset(connection, cardDaoMock);
        cardMock.setStatus(CardStatus.POSTPONED);
        replay(cardMock);
        doChangeStatusTest(cardMock, CardStatus.POSTPONED);
    }

    private void doChangeStatusTest(Card cardMock, CardStatus status) throws DaoException {
        connection.open();
        expectLastCall().once();

        cardDaoMock.update(cardMock);
        expectLastCall().once();
        replay(cardDaoMock);

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        boolean done = dictionaryService.changeStatus("word", status);
        assertTrue(done);
        verify(cardDaoMock);
        verify(connection);
    }

    // test score section

    @Test
    public void testGetScore() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        expect(cardDaoMock.getScore(anyObject(Dictionary.class))).andReturn(createMock(IScore.class));
        replay(dictionaryListMock, cardDaoMock);

        connection.close();
        expectLastCall().once();
        replay(connection);

        IScore score = dictionaryService.getScore(DICTIONARY_NAME);
        assertNotNull(score);
        verify(cardDaoMock);
        verify(connection);
    }

    @Test
    public void testResetScore() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();

        cardDaoMock.resetScore(anyObject(Dictionary.class));
        expectLastCall().once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(dictionaryListMock, cardDaoMock);

        boolean done = dictionaryService.resetScore(DICTIONARY_NAME);
        assertTrue(done);
        verify(cardDaoMock);
        verify(connection);
    }

    @Test
    public void testIncreaseScoreUp() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem cardItemMock = createStrictMock(CardItem.class);
        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(10).once();
        expect(cardItemsCacheMock.get(anyString())).andStubReturn(cardItemMock);
        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        cardMock.setRating(20);
        expectLastCall().once();

        replay(cardItemsCacheMock, cardItemMock, cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp("word", 10);
        assertTrue(done);
        verify(cardItemMock, cardMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testIncreaseScoreUpWhenReach100Percents() throws DaoException {
        connection.open();
        expectLastCall().once();

        CardItem cardItemMock = createStrictMock(CardItem.class);
        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(90);
        expect(cardItemsCacheMock.get(anyString())).andStubReturn(cardItemMock);
        expect(cardItemMock.getCard()).andStubReturn(cardMock);
        cardMock.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock.setRating(100);
        expectLastCall().once();
        replay(cardItemsCacheMock, cardItemMock, cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().once();

        connection.commit();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replay(connection);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp("word", 10);
        assertTrue(done);
        verify(cardItemMock, cardMock, cardDaoMock);
        verify(connection);
    }

    private static Word createWordStub(String word) {
        return new Word(18, word);
    }

    private static Card createCardStub(int id) {
        Card card = new Card();
        card.setId(id);
        card.setDictionaryId(1);
        card.setWordId(18);
        return card;
    }

}