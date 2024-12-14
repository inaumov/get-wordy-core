package get.wordy.core;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.CardHeadlineDao;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EasyMockExtension.class)
public class DictionaryServiceTest {

    private static final int DICTIONARY_ID = 42;
    private static final String DICTIONARY_NAME = "Default";
    private static final OwnerId JOHN_DOE = new OwnerId("john-doe-xyz8w", "individual-user");

    @Mock(name = "dictionaryDao")
    private DictionaryDao dictionaryDaoMock;
    @Mock(name = "cardDao")
    private CardDao cardDaoMock;
    @Mock(name = "wordDao")
    private WordDao wordDaoMock;
    @Mock
    private CardHeadlineDao headlineDaoMock;

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
        expect(dictionaryDaoMock.selectAllByOwnerId(JOHN_DOE)).andReturn(dictionaries);
        expectLastCall().once();
        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries(JOHN_DOE);
        assertEquals(1, list.size());
        verify(dictionaryDaoMock);
    }

    private void replayTxCommited() throws Exception {
        connectionMock.open();
        expectLastCall().atLeastOnce();
        connectionMock.close();
        expectLastCall().atLeastOnce();
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

        expect(dictionaryDaoMock.selectAllByOwnerId(JOHN_DOE)).andStubThrow(new DaoException("selectAll", null));
        replay(dictionaryDaoMock);

        List<Dictionary> list = dictionaryService.getDictionaries(JOHN_DOE);
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

        boolean done = dictionaryService.createDictionary(JOHN_DOE, DICTIONARY_NAME, "http://picture.jpg") != null;
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

        boolean exceptionHappened = dictionaryService.createDictionary(JOHN_DOE, DICTIONARY_NAME, "http://picture.jpg") == null;
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
        expectLastCall().andReturn(1)
                .once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.renameDictionary(JOHN_DOE, DICTIONARY_ID, "nameUpdated");
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
        expectLastCall().andReturn(1)
                .once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.changeDictionaryPicture(JOHN_DOE, DICTIONARY_ID, "http://example.com");
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
                () -> dictionaryService.renameDictionary(JOHN_DOE, DICTIONARY_ID, "nameUpdated"));
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

        boolean done = dictionaryService.renameDictionary(JOHN_DOE, DICTIONARY_ID, "nameUpdated");
        assertFalse(done);
        assertEquals("nameUpdated", dictionaryCapture.getValue().getName());

        verify(dictionaryDaoMock);
    }

    @Test
    public void testDeleteDictionary() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        expect(dictionaryMock.getCardsTotal()).andReturn(0); // override
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        dictionaryDaoMock.delete(DICTIONARY_ID);
        expectLastCall().once();
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.deleteDictionary(JOHN_DOE, DICTIONARY_ID);
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
                () -> dictionaryService.deleteDictionary(JOHN_DOE, DICTIONARY_ID));
        assertNull(exception.getMessage());
    }

    @Test
    public void testDeleteDictionaryWhenCardsPresent() throws Exception {
        Dictionary dictionaryMock = createDictionaryMock();
        expect(dictionaryMock.getCardsTotal()).andReturn(1); // override
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        replayTxWhenException();

        Throwable exception = assertThrows(DictionaryServiceException.class,
                () -> dictionaryService.deleteDictionary(JOHN_DOE, DICTIONARY_ID));
        assertEquals("Cannot delete dictionary with cards", exception.getMessage());
    }

    @Test
    public void testDeleteDictionaryWhenDaoException() throws Exception {
        replayTxRollback();

        Dictionary dictionaryMock = createDictionaryMock();
        expect(dictionaryMock.getCardsTotal()).andReturn(0); // override
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        dictionaryDaoMock.delete(DICTIONARY_ID);
        expectLastCall().andStubThrow(new DaoException("delete", null));
        replay(dictionaryDaoMock);

        boolean done = dictionaryService.deleteDictionary(JOHN_DOE, DICTIONARY_ID);
        assertFalse(done);

        verify(dictionaryDaoMock);
    }

    @Test
    public void testGetCards() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getId()).andReturn(1);
        replay(cardMock);

        expect(headlineDaoMock.getCardsForDictionary(DICTIONARY_ID))
                .andReturn(Collections.singletonList(cardMock));
        expectLastCall().once();
        replay(headlineDaoMock);

        List<Card> cards = dictionaryService.getCards(JOHN_DOE, DICTIONARY_ID);
        assertNotNull(cards);
        assertEquals(1, cards.size());

        verify(headlineDaoMock);
    }

    @Test
    public void testGetCardsForExercise_Full() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        int[] selectedIds = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(anyInt(), anyInt()))
                .andReturn(selectedIds)
                .once();

        int[] inCache = {2, 5, 13};
        for (int cardId : inCache) {
            Card cardMock = strictMock(Card.class);
            addCardToCache(cardId, cardMock);
        }
        expect(headlineDaoMock.getCardsForExercise(selectedIds))
                .andReturn(Collections.nCopies(3, niceMock(Exercise.class)));

        replay(cardDaoMock, headlineDaoMock);

        List<Exercise> cards = dictionaryService.getCardsForExercise(JOHN_DOE, 1, 10);
        assertEquals(3, cards.size());
        verify(cardDaoMock, headlineDaoMock);
    }

    @Test
    public void testGetCardsForExercise_SentencesOnly() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        int[] selectedIds = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(anyInt(), anyInt()))
                .andReturn(selectedIds)
                .once();

        int[] inCache = {1, 2, 3, 5, 8, 13};
        for (int cardId : inCache) {
            int wordId = new Random().nextInt(101);
            Word wordMock = niceMock(Word.class);
            expect(wordMock.getId()).andReturn(wordId);
            replay(wordMock);

            Card cardMock = strictMock(Card.class);
            expect(cardMock.getId()).andReturn(cardId);
            expect(cardMock.getWord()).andReturn(wordMock).times(2);
            cardMock.setWord(wordMock);
            expectLastCall().once();
            replay(cardMock);

            addCardToCache(cardId, cardMock);
        }
        expect(headlineDaoMock.getSentencesFor(selectedIds))
                .andReturn(Map.of(
                        1, Collections.nCopies(1, niceMock(Sentence.class)),
                        3, Collections.nCopies(2, niceMock(Sentence.class)),
                        8, Collections.nCopies(1, niceMock(Sentence.class))
                ));

        replay(cardDaoMock, headlineDaoMock);

        List<Exercise> cards = dictionaryService.getCardsForExercise(JOHN_DOE, 1, 10);
        assertEquals(3, cards.size());
        for (Exercise card : cards) {
            assertTrue(card.getCardId() > 0);
            assertTrue(card.getWordId() > 0);
            assertNotNull(card.getWord());
            assertFalse(card.getSentences().isEmpty());
        }
        verify(cardDaoMock, headlineDaoMock);
    }

    @Test
    void loadCardFullData() throws Exception {
        replayTxCommited();

        int cardId = 1;

        Card cardMock = niceMock(Card.class);
        replay(cardMock);

        headlineDaoMock.getCardById(cardId);
        expectLastCall().andAnswer(() -> cardMock);
        replay(headlineDaoMock);

        Card card = dictionaryService.loadCard(cardId);
        assertNotNull(card);

        verify(cardMock);
        verify(headlineDaoMock);
    }

    @Test
    public void testSaveNewCard() throws Exception {
        replayTxCommited();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = strictMock(Word.class);
        replay(wordMock);

        Word insertedWordMock = strictMock(Word.class);
        expect(insertedWordMock.getId()).andReturn(1);
        expect(insertedWordMock.getValue()).andReturn("banana");
        expect(insertedWordMock.getPartOfSpeech()).andReturn("noun");
        expect(insertedWordMock.getMeaning()).andReturn(null);
        replay(insertedWordMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWord()).andReturn(wordMock);
        cardMock.setDictionaryId(DICTIONARY_ID);
        cardMock.setWordId(1);
        cardMock.setWord(insertedWordMock);
        expect(cardMock.getStrSentences()).andReturn(List.of("Test sentence"));
        expect(cardMock.getCollocations()).andReturn(List.of("Test collocation"));
        replay(cardMock);

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

        int cardId = 1;
        Word wordMock = strictMock(Word.class);
        replay(wordMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWord()).andReturn(wordMock);
        expectLastCall().once();
        replay(cardMock);
        addCardToCache(cardId, cardMock);

        Word wordForUpdMock = strictMock(Word.class);
        expect(wordForUpdMock.getId()).andReturn(1);
        replay(wordForUpdMock);

        Card cardForUpdMock = strictMock(Card.class);
        expect(cardForUpdMock.getId()).andReturn(cardId);
        cardForUpdMock.setDictionaryId(DICTIONARY_ID);
        expectLastCall().once();
        expect(cardForUpdMock.getWord()).andReturn(wordForUpdMock);
        cardForUpdMock.setWord(wordMock);
        expectLastCall().once();
        replay(cardForUpdMock);

        headlineDaoMock.getCardById(cardId);
        expectLastCall().andAnswer(() -> cardMock);
        wordDaoMock.update(wordForUpdMock);
        expectLastCall().andReturn(1);
        cardDaoMock.updateRelations(cardForUpdMock);
        expectLastCall().andAnswer(() -> cardMock);
        replay(headlineDaoMock, wordDaoMock, cardDaoMock);

        Card done = dictionaryService.updateCard(DICTIONARY_ID, cardForUpdMock);
        assertNotNull(done);

        verify(wordMock, cardMock);
        verify(wordDaoMock, cardDaoMock);
    }

    @Test
    public void testDeleteCard() throws Exception {
        // prepare dictionary
        Dictionary dictionaryMock = createDictionaryMock();
        expect(dictionaryMock.getCardsTotal()).andReturn(1); // override
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        replayTxCommited();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andStubReturn(1);
        replay(cardMock);
        addCardToCache(1, cardMock);

        cardDaoMock.delete(1);
        expectLastCall().once();
        replay(cardDaoMock);

        boolean done = dictionaryService.deleteCard(JOHN_DOE, DICTIONARY_ID, 1);
        assertTrue(done);

        verify(cardMock);
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
        expectLastCall().andReturn(1);
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

        Score score = dictionaryService.getScoreSummary(JOHN_DOE, DICTIONARY_ID);
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

        boolean done = dictionaryService.increaseScoreUp(1, new int[]{1, 2, 2, 2, 1}, 10);
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
        expect(wordDaoMock.selectAll(wordIds))
                .andReturn(List.of(wordMock42, wordMock87));
        expectLastCall().once();
        replay(wordDaoMock, cardDaoMock);

        List<Card> done = dictionaryService.generateCards(JOHN_DOE, DICTIONARY_ID, words);
        assertFalse(done.isEmpty());

        verify(wordDaoMock, cardDaoMock);
    }

    private void addDictionaryToCache(Dictionary dictionaryMock) {
        @SuppressWarnings("unchecked")
        var dictionariesCache = (Map<OwnerId, List<Dictionary>>) ReflectionTestUtils.getField(dictionaryService, "dictionariesCache");
        List<Dictionary> dictionaries = new ArrayList<>();
        dictionaries.add(dictionaryMock);
        Objects.requireNonNull(dictionariesCache).put(JOHN_DOE, dictionaries);
    }

    private void addCardToCache(int id, Card cardMock) {
        @SuppressWarnings("unchecked")
        var cardsCache = (Map<Integer, Card>) ReflectionTestUtils.getField(dictionaryService, "cardsCache");
        Objects.requireNonNull(cardsCache).put(id, cardMock);
    }

    private Dictionary createDictionaryMock() {
        Dictionary dictionaryMock = mock(Dictionary.class);
        expect(dictionaryMock.getId()).andReturn(DICTIONARY_ID).anyTimes();
        expect(dictionaryMock.getName()).andReturn(DICTIONARY_NAME);
        return dictionaryMock;
    }

}