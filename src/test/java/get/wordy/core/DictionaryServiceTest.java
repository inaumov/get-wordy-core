package get.wordy.core;

import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.bean.*;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.ConnectionWrapper;
import get.wordy.core.wrapper.Score;
import org.easymock.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;

import java.time.Instant;
import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EasyMockExtension.class)
public class DictionaryServiceTest {

    private static final int DICTIONARY_ID = 42;
    private static final String DICTIONARY_NAME = "Default";

    @Mock(name = "dictionaryDao")
    private DictionaryDao dictionaryDaoMock;
    @Mock(name = "cardDao")
    private CardDao cardDaoMock;
    @Mock(name = "wordDao")
    private WordDao wordDaoMock;
    @Mock(name = "connection")
    private ConnectionWrapper connectionMock;

    @TestSubject
    private DictionaryService dictionaryService;

    @BeforeEach
    public void setUp() {
        EasyMockSupport.injectMocks(this);
    }

    // test dictionary section

    @Test
    public void testGetDictionaries() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        List<Dictionary> dictionaries = Collections.singletonList(dictionaryMock);
        expect(dictionaryDaoMock.selectAll()).andReturn(dictionaries);
        expectLastCall().once();
        replay(dictionaryDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertEquals(1, list.size());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testGetDictionariesWhenDaoException() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        expect(dictionaryDaoMock.selectAll()).andStubThrow(new DaoException("selectAll", null));
        replay(dictionaryDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertTrue(list.isEmpty());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testCreateDictionary() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().andReturn(dictionaryMock);
        replay(dictionaryDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME, "http://picture.jpg") != null;
        assertTrue(done);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testCreateDictionaryWhenDaoException() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("insert", null));
        replay(dictionaryDaoMock);

        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean exceptionHappened = dictionaryService.createDictionary(DICTIONARY_NAME, "http://picture.jpg") == null;
        assertTrue(exceptionHappened);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionary() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        addDictionaryToCache(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andReturn(dictionaryMock)
                .once();
        replay(dictionaryDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated");
        assertTrue(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionaryWhenNotFound() {
        Throwable exception = assertThrows(DictionaryNotFoundException.class,
                () -> dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated"));
        assertNull(exception.getMessage());
    }

    @Test
    public void testRenameDictionaryWhenDaoException() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        addDictionaryToCache(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("rename", null));

        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(dictionaryDaoMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated");
        assertFalse(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testRemoveDictionary() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        addDictionaryToCache(dictionaryMock);
        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().once();

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(dictionaryDaoMock);

        boolean done = dictionaryService.removeDictionary(DICTIONARY_ID);
        assertTrue(done);
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testRemoveDictionaryWhenNotFound() {
        Throwable exception = assertThrows(DictionaryNotFoundException.class,
                () -> dictionaryService.removeDictionary(DICTIONARY_ID));
        assertNull(exception.getMessage());
    }

    @Test
    public void testRemoveDictionaryWhenDaoException() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().andStubThrow(new DaoException("insert", null));

        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(dictionaryDaoMock);

        boolean done = dictionaryService.removeDictionary(DICTIONARY_ID);
        assertFalse(done);
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testGetCards() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = strictMock(Word.class);
        expect(wordMock.getId()).andReturn(1);
        expect(wordMock.getValue()).andReturn("word");
        replay(wordMock);

        Context contextMock = niceMock(Context.class);
        expect(contextMock.getId()).andReturn(1);
        replay(contextMock);

        Collocation collocationMock = niceMock(Collocation.class);
        expect(collocationMock.getId()).andReturn(1);
        replay(collocationMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andReturn(1).times(2);
        cardMock.setWord(wordMock);
        cardMock.addContexts(List.of(contextMock));
        expectLastCall().anyTimes();
        cardMock.addCollocations(List.of(collocationMock));
        expectLastCall().anyTimes();
        expect(cardMock.getId()).andReturn(1);
        replay(cardMock);

        expect(cardDaoMock.selectCardsForDictionary(anyObject(Dictionary.class)))
                .andReturn(Collections.singletonList(cardMock));
        expectLastCall().once();
        expect(cardDaoMock.getContextsFor(cardMock))
                .andReturn(Collections.singletonList(contextMock));
        expectLastCall().once();
        expect(cardDaoMock.getCollocationsFor(cardMock))
                .andReturn(Collections.singletonList(collocationMock));
        expectLastCall().once();

        replay(cardDaoMock);

        expect(wordDaoMock.selectAll())
                .andReturn(Collections.singletonList(wordMock));
        expectLastCall().once();
        replay(wordDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        List<Card> cards = dictionaryService.getCards(DICTIONARY_ID);
        assertNotNull(cards);
        assertFalse(cards.isEmpty());
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testGetCardsForExercise() throws Exception {
        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        int[] ids = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(anyInt(), anyInt()))
                .andReturn(ids)
                .once();

        int[] all = {1, 2, 3, 5, 8, 13};
        for (int id : all) {
            Card cardMock = strictMock(Card.class);
            addCardToCache(id, cardMock);
        }

        replay(cardDaoMock);

        List<Card> cards = dictionaryService.getCardsForExercise(1, 6);
        assertEquals(3, cards.size());
    }

    @Test
    public void testSaveNewCard() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = strictMock(Word.class);
        replay(wordMock);

        Context contextMock = strictMock(Context.class);
        contextMock.setWordId(1);
        replay(contextMock);

        Collocation collocationMock = strictMock(Collocation.class);
        collocationMock.setWordId(1);
        replay(collocationMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWord()).andReturn(wordMock);
        cardMock.setDictionaryId(DICTIONARY_ID);
        cardMock.setWordId(1);
        cardMock.setInsertedAt(anyObject(Instant.class));
        expect(cardMock.getContexts()).andReturn(List.of(contextMock));
        expect(cardMock.getCollocations()).andReturn(List.of(collocationMock));
        replay(cardMock);

        Word insertedWordMock = strictMock(Word.class);
        expect(insertedWordMock.getId()).andReturn(1);
        replay(insertedWordMock);

        Card insertedCardMock = strictMock(Card.class);
        expect(insertedCardMock.getId()).andReturn(1);
        replay(insertedCardMock);

        wordDaoMock.insert(wordMock);
        expectLastCall().andAnswer(() -> insertedWordMock);
        cardDaoMock.insert(cardMock);
        expectLastCall().andAnswer(() -> insertedCardMock);
        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        Card done = dictionaryService.addCard(DICTIONARY_ID, cardMock);

        assertNotNull(done);
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testUpdateCard() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Word wordMock = strictMock(Word.class);
        replay(wordMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWord()).andReturn(wordMock);
        replay(cardMock);
        addCardToCache(1, cardMock);

        Word wordForUpdMock = strictMock(Word.class);
        expect(wordForUpdMock.getId()).andReturn(1);
        replay(wordForUpdMock);

        Card cardForUpdMock = strictMock(Card.class);
        expect(cardForUpdMock.getId()).andReturn(1);
        expect(cardForUpdMock.getWord()).andReturn(wordForUpdMock);
        replay(cardForUpdMock);

        wordDaoMock.update(wordForUpdMock);
        expectLastCall().andAnswer(() -> wordMock);
        cardDaoMock.update(cardForUpdMock);
        expectLastCall().andAnswer(() -> cardMock);
        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        Card done = dictionaryService.updateCard(DICTIONARY_ID, cardForUpdMock);

        assertNotNull(done);
        verify(wordMock, cardMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testRemoveCard() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andStubReturn(1);
        Word wordMock = strictMock(Word.class);
        expect(cardMock.getWord()).andStubReturn(wordMock);
        replay(cardMock, wordMock);
        addCardToCache(1, cardMock);

        cardDaoMock.delete(cardMock);
        expectLastCall().once();
        replay(cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);


        boolean done = dictionaryService.removeCard(1);
        assertTrue(done);
        verify(cardMock, wordMock);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testChangeStatusWhenThrowDaoException() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = strictMock(Card.class);
        cardMock.setStatus(CardStatus.DEFAULT_STATUS);
        cardMock.setScore(0);
        expect(cardMock.getScore()).andReturn(0);
        replay(cardMock);

        addCardToCache(1, cardMock);

        cardDaoMock.updateStatus(1, CardStatus.DEFAULT_STATUS, 0);
        expectLastCall().andStubThrow(new DaoException("changeStatus", null));
        replay(cardDaoMock);

        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.changeStatus(1, CardStatus.DEFAULT_STATUS);
        assertFalse(done);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @ParameterizedTest
    @CsvSource(value = {"EDIT,0", "LEARNT,100"})
    public void testChangeStatusWhenResetRating(CardStatus cardStatus, int score) throws Exception {
        Card cardMock = strictMock(Card.class);
        cardMock.setStatus(cardStatus);
        cardMock.setScore(score);
        expect(cardMock.getScore()).andReturn(score);
        replay(cardMock);
        addCardToCache(1, cardMock);
        doChangeStatusTest(1, cardStatus, score);
    }

    @ParameterizedTest
    @CsvSource(value = {"TO_LEARN,50", "POSTPONED,20"})
    public void testChangeStatusWhenKeepRating(CardStatus cardStatus, int score) throws Exception {
        Card cardMock = strictMock(Card.class);
        cardMock.setStatus(cardStatus);
        expect(cardMock.getScore()).andReturn(score);
        replay(cardMock);
        addCardToCache(1, cardMock);
        doChangeStatusTest(1, cardStatus, score);
    }

    private void doChangeStatusTest(int cardId, CardStatus status, int score) throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        cardDaoMock.updateStatus(cardId, status, score);
        expectLastCall().once();
        replay(cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.changeStatus(cardId, status);
        assertTrue(done);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testGetScore() throws Exception {
        Dictionary dictionaryMock = createDictionaryMock();
        addDictionaryToCache(dictionaryMock);
        replay(dictionaryMock);

        connectionMock.open();
        expectLastCall().once();

        expect(cardDaoMock.getScore(DICTIONARY_ID)).andReturn(Map.of("EDIT", 1, "LEARNT", 3));
        replay(cardDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        Score score = dictionaryService.getScore(DICTIONARY_ID);
        assertEquals(1, score.getEditCnt());
        assertEquals(0, score.getToLearnCnt());
        assertEquals(0, score.getPostponedCnt());
        assertEquals(3, score.getLearntCnt());
        assertEquals(4, score.getTotalCount());
        assertNotNull(score);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testResetScore() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        cardDaoMock.resetScore(dictionaryMock);
        expectLastCall().once();

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(cardDaoMock);

        boolean done = dictionaryService.resetScore(DICTIONARY_ID);
        assertTrue(done);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testIncreaseScoreUp() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getScore()).andReturn(10).once();
        cardMock.setScore(20);
        expectLastCall().once();
        replay(cardMock);
        addCardToCache(1, cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().andReturn(cardMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp(1, 10);
        assertTrue(done);
        verify(cardMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testIncreaseScoreUpWhenReach100Percents() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getScore()).andReturn(90).once();
        cardMock.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock.setScore(100);
        expectLastCall().once();
        replay(cardMock);
        addCardToCache(1, cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().andReturn(cardMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp(1, 10);
        assertTrue(done);
        verify(cardMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testGenerateCards() throws Exception {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock42 = strictMock(Word.class);
        expect(wordMock42.getId()).andReturn(42).times(2);
        expect(wordMock42.getValue()).andReturn("singleton").anyTimes();
        replay(wordMock42);

        Word wordMock87 = strictMock(Word.class);
        expect(wordMock87.getId()).andReturn(87).times(2);
        expect(wordMock87.getValue()).andReturn("generated").anyTimes();
        replay(wordMock87);

        Set<String> words = Set.of("singleton", "generated");
        Set<Integer> wordIds = Set.of(42, 87);
        expect(wordDaoMock.generate(words)).andReturn(wordIds).once();
        expect(cardDaoMock.generateEmptyCards(anyInt(), eq(wordIds)))
                .andReturn(Set.of(15, 16)).once();
        expect(wordDaoMock.selectAll())
                .andReturn(List.of(wordMock42, wordMock87));
        expectLastCall().once();
        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        List<Card> done = dictionaryService.generateCards(DICTIONARY_ID, words);
        assertFalse(done.isEmpty());
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    private void addDictionaryToCache(Dictionary dictionaryMock) throws Exception {
        Try<Object> dictionaryList = ReflectionUtils.tryToReadFieldValue(DictionaryService.class, "dictionaryList", dictionaryService);
        List<Dictionary> o = (List<Dictionary>) dictionaryList.get();
        o.add(dictionaryMock);
    }

    private void addCardToCache(int id, Card cardMock) throws Exception {
        Try<Object> dictionaryList = ReflectionUtils.tryToReadFieldValue(DictionaryService.class, "cardsCache", dictionaryService);
        Map<Integer, Card> o = (Map<Integer, Card>) dictionaryList.get();
        o.put(id, cardMock);
    }

    private Dictionary createDictionaryMock() {
        Dictionary dictionaryMock = strictMock(Dictionary.class);
        expect(dictionaryMock.getId()).andReturn(DICTIONARY_ID).atLeastOnce();
        expect(dictionaryMock.getName()).andReturn(DICTIONARY_NAME).atLeastOnce();
        return dictionaryMock;
    }

}