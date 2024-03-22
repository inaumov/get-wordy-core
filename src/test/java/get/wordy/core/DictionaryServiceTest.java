package get.wordy.core;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.bean.wrapper.Score;
import org.easymock.*;
import org.junit.jupiter.api.AfterEach;
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
    private LocalTxManager connectionMock;

    @TestSubject
    private DictionaryService dictionaryService;

    @BeforeEach
    public void setUp() {
        EasyMockSupport.injectMocks(this);
    }

    @AfterEach
    void tearDown() {
        verify(connectionMock);
    }

    // test dictionary section

    @Test
    public void testGetDictionaries() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        List<Dictionary> dictionaries = Collections.singletonList(dictionaryMock);
        expect(dictionaryDaoMock.selectAll()).andReturn(dictionaries);
        expectLastCall().once();
        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertEquals(1, list.size());
        verify(dictionaryDaoMock);
    }

    private void replayTxCommited() throws Exception {
        connectionMock.open();
        expectLastCall().atLeastOnce();
        connectionMock.close();
        expectLastCall().once();
        connectionMock.commit();
        expectLastCall().atLeastOnce();

        replay(connectionMock);
    }

    private void replayTxWhenException() throws Exception {
        connectionMock.open();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();

        replay(connectionMock);
    }

    private void replayTxRollback() throws Exception {
        connectionMock.open();
        expectLastCall().once();
        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();

        replay(connectionMock);
    }

    @Test
    public void testGetDictionariesWhenDaoException() throws Exception {
        replayTxWhenException();

        expect(dictionaryDaoMock.selectAll()).andStubThrow(new DaoException("selectAll", null));
        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries();
        assertTrue(list.isEmpty());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testCreateDictionary() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().andReturn(dictionaryMock);
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME, "http://picture.jpg") != null;
        assertTrue(done);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testCreateDictionaryWhenDaoException() throws Exception {
        replayTxRollback();

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.insert(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("insert", null));
        replay(dictionaryDaoMock);

        boolean exceptionHappened = dictionaryService.createDictionary(DICTIONARY_NAME, "http://picture.jpg") == null;
        assertTrue(exceptionHappened);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionary() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        dictionaryMock.setName("nameUpdated");
        addDictionaryToCache(dictionaryMock);
        replay(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andReturn(dictionaryMock)
                .once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated");
        assertTrue(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testChangeDictionaryPicture() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        dictionaryMock.setPicture("http://example.com");
        addDictionaryToCache(dictionaryMock);
        replay(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andReturn(dictionaryMock)
                .once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.changeDictionaryPicture(DICTIONARY_ID, "http://example.com");
        assertTrue(done);
        assertEquals("http://example.com", dictionaryCapture.getValue().getPicture());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionaryWhenNotFound() throws Exception {
        // expect commit when get a dictionary
        connectionMock.commit();
        expectLastCall().atLeastOnce();
        replayTxWhenException();

        Throwable exception = assertThrows(DictionaryNotFoundException.class,
                () -> dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated"));
        assertNull(exception.getMessage());
    }

    @Test
    public void testRenameDictionaryWhenDaoException() throws Exception {
        replayTxRollback();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        addDictionaryToCache(dictionaryMock);

        Capture<Dictionary> dictionaryCapture = Capture.newInstance();
        dictionaryDaoMock.update(capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DaoException("rename", null));
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated");
        assertFalse(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testDeleteDictionary() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);

        addDictionaryToCache(dictionaryMock);
        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.deleteDictionary(DICTIONARY_ID);
        assertTrue(done);

        verify(dictionaryDaoMock);
    }

    @Test
    public void testDeleteDictionaryWhenNotFound() throws Exception {
        // expect commit when get a dictionary
        connectionMock.commit();
        expectLastCall().atLeastOnce();
        replayTxWhenException();

        Throwable exception = assertThrows(DictionaryNotFoundException.class,
                () -> dictionaryService.deleteDictionary(DICTIONARY_ID));
        assertNull(exception.getMessage());
    }

    @Test
    public void testDeleteDictionaryWhenDaoException() throws Exception {
        replayTxRollback();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        dictionaryDaoMock.delete(dictionaryMock);
        expectLastCall().andStubThrow(new DaoException("insert", null));
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.deleteDictionary(DICTIONARY_ID);
        assertFalse(done);

        verify(dictionaryDaoMock);
    }

    @Test
    public void testGetCards() throws Exception {
        replayTxCommited();

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
        cardMock.setContexts(List.of(contextMock));
        expectLastCall().anyTimes();
        cardMock.setCollocations(List.of(collocationMock));
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

        List<Card> cards = dictionaryService.getCards(DICTIONARY_ID);
        assertNotNull(cards);
        assertFalse(cards.isEmpty());

        verify(wordDaoMock, cardDaoMock);
    }

    @Test
    public void testGetCardsForExercise() throws Exception {
        replayTxCommited();

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
    void loadCardFullData() throws Exception {
        replayTxCommited();

        int wordId = 1;
        int cardId = 1;

        Word wordMock = niceMock(Word.class);
        expect(wordMock.getId()).andReturn(wordId);
        replay(wordMock);

        Card cardMock = niceMock(Card.class);
        expect(cardMock.getId()).andReturn(cardId).anyTimes();
        expect(cardMock.getWord()).andReturn(null);
        expect(cardMock.getWordId()).andReturn(wordId);
        cardMock.setWord(wordMock);
        expect(cardMock.getContexts()).andReturn(Collections.emptyList());
        expect(cardMock.getCollocations()).andReturn(Collections.emptyList());
        replay(cardMock);

        wordDaoMock.selectById(wordId);
        expectLastCall().andAnswer(() -> wordMock);
        cardDaoMock.selectById(cardId);
        expectLastCall().andAnswer(() -> cardMock);
        cardDaoMock.getContextsFor(cardMock);
        expectLastCall().andAnswer(() -> List.of(new Context()));
        cardDaoMock.getCollocationsFor(cardMock);
        expectLastCall().andAnswer(() -> List.of(new Collocation()));
        replay(wordDaoMock, cardDaoMock);

        Card card = dictionaryService.loadCard(cardId);
        assertNotNull(card);

        verify(cardMock);
        verify(wordDaoMock, cardDaoMock);
    }

    @Test
    public void testSaveNewCard() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = strictMock(Word.class);
        replay(wordMock);

        Context contextMock = strictMock(Context.class);
        contextMock.setCardId(1);
        replay(contextMock);

        Collocation collocationMock = strictMock(Collocation.class);
        collocationMock.setCardId(1);
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

        Card done = dictionaryService.addCard(DICTIONARY_ID, cardMock);
        assertNotNull(done);

        verify(wordDaoMock, cardDaoMock);
    }

    @Test
    public void testUpdateCard() throws Exception {
        replayTxCommited();

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

        Card done = dictionaryService.updateCard(DICTIONARY_ID, cardForUpdMock);
        assertNotNull(done);

        verify(wordMock, cardMock);
        verify(wordDaoMock, cardDaoMock);
    }

    @Test
    public void testDeleteCard() throws Exception {
        replayTxCommited();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andStubReturn(1);
        Word wordMock = strictMock(Word.class);
        expect(cardMock.getWord()).andStubReturn(wordMock);
        replay(cardMock, wordMock);
        addCardToCache(1, cardMock);

        cardDaoMock.delete(cardMock);
        expectLastCall().once();
        replay(cardDaoMock);

        boolean done = dictionaryService.deleteCard(1);
        assertTrue(done);

        verify(cardMock, wordMock);
        verify(cardDaoMock);
    }

    @Test
    public void testChangeStatusWhenThrowDaoException() throws Exception {
        replayTxRollback();

        Card cardMock = strictMock(Card.class);
        cardMock.setStatus(CardStatus.DEFAULT_STATUS);
        cardMock.setScore(0);
        expect(cardMock.getScore()).andReturn(0);
        replay(cardMock);

        addCardToCache(1, cardMock);

        cardDaoMock.updateStatus(1, CardStatus.DEFAULT_STATUS, 0);
        expectLastCall().andStubThrow(new DaoException("changeStatus", null));
        replay(cardDaoMock);

        boolean done = dictionaryService.changeStatus(1, CardStatus.DEFAULT_STATUS);
        assertFalse(done);

        verify(cardDaoMock);
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

    private void doChangeStatusTest(int cardId, CardStatus status, int score) throws Exception {
        replayTxCommited();

        cardDaoMock.updateStatus(cardId, status, score);
        expectLastCall().once();
        replay(cardDaoMock);

        boolean done = dictionaryService.changeStatus(cardId, status);
        assertTrue(done);

        verify(cardDaoMock);
    }

    @Test
    public void getScoreSummary() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        addDictionaryToCache(dictionaryMock);
        replay(dictionaryMock);

        expect(cardDaoMock.getScoreSummary(DICTIONARY_ID)).andReturn(Map.of("EDIT", 1, "LEARNT", 3));
        replay(cardDaoMock);

        Score score = dictionaryService.getScoreSummary(DICTIONARY_ID);
        assertEquals(1, score.getEditCnt());
        assertEquals(0, score.getToLearnCnt());
        assertEquals(0, score.getPostponedCnt());
        assertEquals(3, score.getLearntCnt());
        assertEquals(4, score.getTotalCount());
        assertNotNull(score);

        verify(cardDaoMock);
    }

    @Test
    public void testResetScore() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        cardDaoMock.resetScore(1, CardStatus.TO_LEARN);
        expectLastCall().once();

        replay(cardDaoMock);

        boolean done = dictionaryService.resetScore(1);
        assertTrue(done);

        verify(cardDaoMock);
    }

    @Test
    public void testIncreaseScoreUpAndOneReaches100Percents() throws Exception {
        replayTxCommited();

        Card cardMock1 = strictMock(Card.class);
        expect(cardMock1.getScore()).andReturn(10).once();
        cardMock1.setScore(20);
        expectLastCall().once();
        Card cardMock2 = strictMock(Card.class);
        expect(cardMock2.getScore()).andReturn(95).once();
        cardMock2.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock2.setScore(100);
        expectLastCall().once();
        replay(cardMock1, cardMock2);
        addCardToCache(1, cardMock1);
        addCardToCache(2, cardMock2);

        cardDaoMock.batchUpdateScores(List.of(cardMock1, cardMock2));
        expectLastCall().once();
        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp(1, new int[]{1, 2}, 10);
        assertTrue(done);

        verify(cardMock1, cardDaoMock);
    }

    @Test
    public void testGenerateCards() throws Exception {
        replayTxCommited();

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

        List<Card> done = dictionaryService.generateCards(DICTIONARY_ID, words);
        assertFalse(done.isEmpty());

        verify(wordDaoMock, cardDaoMock);
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
        Dictionary dictionaryMock = mock(Dictionary.class);
        expect(dictionaryMock.getId()).andReturn(DICTIONARY_ID).anyTimes();
        expect(dictionaryMock.getName()).andReturn(DICTIONARY_NAME);
        return dictionaryMock;
    }

}