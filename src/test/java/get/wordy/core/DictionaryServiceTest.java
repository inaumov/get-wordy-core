package get.wordy.core;

import get.wordy.core.api.IDictionaryService;
import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.ICardDao;
import get.wordy.core.api.dao.IDictionaryDao;
import get.wordy.core.api.dao.IWordDao;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Word;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.db.ConnectionFactory;
import get.wordy.core.bean.Card;
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

    @Mock(name = "cardsCache", type = MockType.NICE)
    private HashMap<String, Card> cardsCacheMock;

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

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME) != null;
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

        boolean exceptionHappened = dictionaryService.createDictionary(DICTIONARY_NAME) == null;
        assertTrue(exceptionHappened);
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

        cardsCacheMock.putAll(anyObject(HashMap.class));
        expectLastCall().once();
        replay(cardDaoMock, wordDaoMock, cardsCacheMock);

        connection.close();
        expectLastCall().once();
        replay(connection);

        Map<String, Card> cards = dictionaryService.getCards(DICTIONARY_NAME);
        assertNotNull(cards);
        assertFalse(cards.isEmpty());
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
            Card cardStub = createCardStub(id);
            cardStub.setWord(createWordStub("word" + id));
            list.add(cardStub);
        }

        expect(cardsCacheMock.values()).andReturn(list).times(6);
        replay(cardsCacheMock);

        replay(dictionaryMock, dictionaryListMock, dictionaryIteratorMock, cardDaoMock);

        Map<String, Card> cards = dictionaryService.getCardsForExercise(DICTIONARY_NAME, 6);
        assertEquals(6, cards.size());
    }

    @Test
    public void testSaveCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();
        replay(dictionaryMock, dictionaryListMock, dictionaryIteratorMock);

        Card cardMock = createNiceMock(Card.class);
        expect(cardsCacheMock.put("word", cardMock)).andStubReturn(cardMock);
        replay(cardsCacheMock);

        Word wordMock = createNiceMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");
        expect(wordMock.getId()).andReturn(1).times(2);

        cardMock.setWordId(anyInt());
        expectLastCall();
        cardMock.setDictionaryId(anyInt());
        expectLastCall();

        expect(cardMock.getId()).andReturn(0).andStubReturn(1);
        expect(cardMock.getWord()).andStubReturn(wordMock);
        replay(cardMock, wordMock);

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

        boolean done = dictionaryService.saveOrUpdateCard(cardMock, DICTIONARY_NAME);
        assertTrue(done);
        verify(cardMock, wordMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testUpdateCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        expect(dictionaryListMock.iterator()).andReturn(dictionaryIteratorMock).once();
        replay(dictionaryMock, dictionaryListMock, dictionaryIteratorMock);

        Card oldCardMock = createNiceMock(Card.class);
        Word oldWordMock = createNiceMock(Word.class);

        expect(oldCardMock.getWord()).andReturn(oldWordMock).times(2);
        expect(oldWordMock.getValue()).andReturn("word");
        replay(oldCardMock, oldWordMock);

        Collection list = Collections.singletonList(oldCardMock);
        expect(cardsCacheMock.values()).andReturn(list).once();

        Card cardMock = createNiceMock(Card.class);
        expect(cardMock.getId()).andReturn(1);
        Word wordMock = createNiceMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");
        expect(cardMock.getWord()).andReturn(wordMock).once();
        replay(cardMock, wordMock);

        expect(cardsCacheMock.remove("word")).andStubReturn(oldCardMock);
        expect(cardsCacheMock.put("word", cardMock)).andStubReturn(cardMock);
        replay(cardsCacheMock);

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

        boolean done = dictionaryService.saveOrUpdateCard(cardMock, DICTIONARY_NAME);

        assertTrue(done);
        verify(cardMock, wordMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testRemoveCard() throws DaoException {
        connection.open();
        expectLastCall().once();

        Card cardMock = createMock(Card.class);
        expect(cardsCacheMock.get(anyString())).andStubReturn(cardMock);
        expect(cardsCacheMock.remove(anyString())).andStubReturn(cardMock);
        replay(cardsCacheMock);

        Word wordMock = createMock(Word.class);
        expect(wordMock.getValue()).andReturn("word");
        expect(cardMock.getWord()).andStubReturn(wordMock);
        replay(cardMock, wordMock);

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
        verify(cardMock, wordMock);
        verify(cardDaoMock);
        verify(connection);
    }


    // test status section

    @Test
    public void testChangeStatusWhenThrowDaoException() throws DaoException {
        connection.open();
        expectLastCall().once();

        Card cardMock = createMock(Card.class);
        expect(cardsCacheMock.get(anyString())).andStubReturn(cardMock);
        replay(cardsCacheMock);

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
        Card cardMock = createMock(Card.class);
        expect(cardsCacheMock.get(anyString())).andStubReturn(cardMock);
        replay(cardsCacheMock);

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

        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(10).once();
        expect(cardsCacheMock.get(anyString())).andStubReturn(cardMock);
        cardMock.setRating(20);
        expectLastCall().once();

        replay(cardsCacheMock, cardMock);

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
        verify(cardMock, cardDaoMock);
        verify(connection);
    }

    @Test
    public void testIncreaseScoreUpWhenReach100Percents() throws DaoException {
        connection.open();
        expectLastCall().once();

        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(90);
        expect(cardsCacheMock.get(anyString())).andStubReturn(cardMock);
        cardMock.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock.setRating(100);
        expectLastCall().once();
        replay(cardsCacheMock, cardMock);

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
        verify(cardMock, cardDaoMock);
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