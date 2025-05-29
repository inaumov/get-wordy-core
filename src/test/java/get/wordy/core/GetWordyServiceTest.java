package get.wordy.core;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.exception.VocabNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.core.dao.impl.VocabularyDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.bean.wrapper.Score;
import org.easymock.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.capture;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EasyMockExtension.class)
public class GetWordyServiceTest {

    private static final int VOCAB_ID = 42;
    private static final String VOCAB_NAME = "Default";
    private static final String PICTURE_JPG = "http://picture.jpg";
    private static final OwnerId JOHN_DOE = new OwnerId("john-doe-xyz8w", "individual-user");

    @Mock(name = "vocabularyDao")
    private VocabularyDao vocabularyDaoMock;
    @Mock(name = "cardDao")
    private CardDao cardDaoMock;
    @Mock(name = "wordDao")
    private WordDao wordDaoMock;
    @Mock
    private CardHeadlineDao headlineDaoMock;

    @Mock(name = "connection")
    private LocalTxManager connectionMock;

    @TestSubject
    private GetWordyService sut;

    @BeforeEach
    public void setUp() {
        EasyMockSupport.injectMocks(this);
    }

    @Test
    public void testGetVocabularies() {
        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);

        List<Vocabulary> vocabularies = Collections.singletonList(vocabularyMock);
        expect(vocabularyDaoMock.selectAll(JOHN_DOE)).andReturn(vocabularies);
        expectLastCall().once();
        replay(vocabularyDaoMock);

        List<Vocabulary> list = sut.getVocabularies(JOHN_DOE);
        assertEquals(1, list.size());
        verify(vocabularyDaoMock);
    }

    @Test
    public void testGetVocabulariesWhenException() {

        expect(vocabularyDaoMock.selectAll(JOHN_DOE))
                .andStubThrow(new DataAccessException("selectAll", null) {
                });
        replay(vocabularyDaoMock);

        List<Vocabulary> list = sut.getVocabularies(JOHN_DOE);
        assertTrue(list.isEmpty());

        verify(vocabularyDaoMock);
    }

    @Test
    public void testCreateVocabulary() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);

        Capture<Vocabulary> dictionaryCapture = Capture.newInstance();
        vocabularyDaoMock.insert(eq(JOHN_DOE), capture(dictionaryCapture));
        expectLastCall().andReturn(vocabularyMock);
        replay(vocabularyDaoMock);

        Vocabulary vocabulary = sut.createVocabulary(JOHN_DOE, VOCAB_NAME, PICTURE_JPG);
        assertNotNull(vocabulary);
        assertEquals(VOCAB_NAME, dictionaryCapture.getValue().getName());

        verify(vocabularyDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testCreateVocabularyWhenException() throws Exception {
        replayTxRollback();

        Capture<Vocabulary> dictionaryCapture = Capture.newInstance();
        vocabularyDaoMock.insert(eq(JOHN_DOE), capture(dictionaryCapture));
        expectLastCall().andStubThrow(new DataAccessException("insert", null) {
        });
        replay(vocabularyDaoMock);

        boolean exceptionHappened = sut.createVocabulary(JOHN_DOE, VOCAB_NAME, "http://picture.jpg") == null;
        assertTrue(exceptionHappened);
        assertEquals(VOCAB_NAME, dictionaryCapture.getValue().getName());

        verify(vocabularyDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testRenameVocabulary() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getName())
                .andReturn("nameBefore")
                .andReturn("nameUpdated");
        addVocabularyToCache(vocabularyMock);
        replay(vocabularyMock);

        vocabularyDaoMock.rename(vocabularyMock.getVocabId(), "nameUpdated");
        expectLastCall().andReturn(vocabularyMock);
        replay(vocabularyDaoMock);

        boolean done = sut.renameVocabulary(JOHN_DOE, VOCAB_ID, "nameUpdated");
        assertTrue(done);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testChangeVocabularyPicture() throws Exception {
        String newPictureUrl = "http://example.com";
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getPictureUrl()).andReturn(PICTURE_JPG);
        vocabularyMock.setPictureUrl(newPictureUrl);
        addVocabularyToCache(vocabularyMock);
        replay(vocabularyMock);

        vocabularyDaoMock.updatePicture(VOCAB_ID, newPictureUrl);
        expectLastCall().andReturn(1)
                .once();
        replay(vocabularyDaoMock);

        boolean done = sut.changeVocabularyPicture(JOHN_DOE, VOCAB_ID, newPictureUrl);
        assertTrue(done);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testRenameVocabularyWhenNotFound() {
        replayTxShouldNotStart();

        expect(vocabularyDaoMock.selectById(VOCAB_ID))
                .andReturn(Optional.empty());
        expectLastCall().once();
        replay(vocabularyDaoMock);

        Throwable exception = assertThrows(VocabNotFoundException.class,
                () -> sut.renameVocabulary(JOHN_DOE, VOCAB_ID, "nameUpdated"));
        assertTrue(exception.getMessage().startsWith("Vocabulary with id = "));

        verify(vocabularyDaoMock);
    }

    @Test
    public void testMakeVocabularyShared() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.isShared())
                .andReturn(false)
                .andReturn(true);
        addVocabularyToCache(vocabularyMock);
        replay(vocabularyMock);

        vocabularyDaoMock.updateIsShared(VOCAB_ID, true);
        expectLastCall().andReturn(vocabularyMock);
        replay(vocabularyDaoMock);

        boolean done = sut.updateSharing(JOHN_DOE, VOCAB_ID, true);
        assertTrue(done);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testDeleteVocabulary() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getWordsTotal()).andReturn(0); // override
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        vocabularyDaoMock.deleteVocabularyById(VOCAB_ID);
        expectLastCall().andReturn(1).once();
        replay(vocabularyDaoMock);

        boolean done = sut.deleteVocabulary(JOHN_DOE, VOCAB_ID);
        assertTrue(done);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testDeleteVocabularyWhenNotFound() {
        replayTxShouldNotStart();

        expect(vocabularyDaoMock.selectById(VOCAB_ID))
                .andReturn(Optional.empty());
        expectLastCall().once();
        replay(vocabularyDaoMock);

        Throwable exception = assertThrows(VocabNotFoundException.class,
                () -> sut.deleteVocabulary(JOHN_DOE, VOCAB_ID));
        assertTrue(exception.getMessage().startsWith("Vocabulary with id = 42 not found for owner id"));
    }

    @Test
    public void testDeleteVocabularyWhenWordsPresent() throws Exception {
        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getWordsTotal()).andReturn(1).anyTimes();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        replayTxWhenException();

        Throwable exception = assertThrows(DictionaryServiceException.class,
                () -> sut.deleteVocabulary(JOHN_DOE, VOCAB_ID));
        assertEquals("Cannot delete vocabulary with words (not empty)", exception.getMessage());
    }

    @Test
    public void testDeleteVocabularyWhenException() throws Exception {
        replayTxRollback();

        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getWordsTotal()).andReturn(0); // override
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        vocabularyDaoMock.deleteVocabularyById(VOCAB_ID);
        expectLastCall().andStubThrow(new DataAccessException("delete", null) {
        });
        replay(vocabularyDaoMock);

        boolean done = sut.deleteVocabulary(JOHN_DOE, VOCAB_ID);
        assertFalse(done);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testGetCards() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getId()).andReturn(1);
        expect(cardMock.getVocabId()).andReturn(1);
        expect(cardMock.getWordId()).andReturn(1);
        replay(cardMock);

        expect(headlineDaoMock.getCards(VOCAB_ID))
                .andReturn(Collections.singletonList(cardMock));
        expectLastCall().once();
        replay(headlineDaoMock);

        List<Card> cards = sut.getCards(JOHN_DOE, VOCAB_ID);
        assertNotNull(cards);
        assertEquals(1, cards.size());

        verify(headlineDaoMock);
    }

    @Test
    public void testGetCardsForExercise_Full() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        int[] selectedIds = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(eq(JOHN_DOE), anyInt(), anyInt()))
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

        List<Exercise> cards = sut.getCardsForExercise(JOHN_DOE, 1, 10);
        assertEquals(3, cards.size());
        verify(cardDaoMock, headlineDaoMock);
    }

    @Test
    public void testGetCardsForExercise_SentencesOnly() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        int[] selectedIds = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(eq(JOHN_DOE), anyInt(), anyInt()))
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

        List<Exercise> cards = sut.getCardsForExercise(JOHN_DOE, 1, 10);
        assertEquals(3, cards.size());
        for (Exercise card : cards) {
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

        Card card = sut.loadCard(cardId);
        assertNotNull(card);

        verify(cardMock);
        verify(headlineDaoMock);
    }

    @Test
    public void testSaveNewCard() throws Exception {
        replayTxCommited();

        Card insertedCardMock = strictMock(Card.class);
        expect(insertedCardMock.getId()).andReturn(1);
        replay(insertedCardMock);

        Capture<Card> cardCapture = Capture.newInstance();
        cardDaoMock.insert(eq(JOHN_DOE), capture(cardCapture));
        expectLastCall().andAnswer(() -> insertedCardMock);
        replay(cardDaoMock);

        Card done = sut.addCard(JOHN_DOE, VOCAB_ID, 105);
        assertNotNull(done);
        Assertions.assertEquals(VOCAB_ID, cardCapture.getValue().getVocabId());
        Assertions.assertEquals(105, cardCapture.getValue().getWordId());
        Assertions.assertEquals(CardStatus.TO_LEARN, cardCapture.getValue().getStatus());

        verify(cardDaoMock);
    }

    @Test
    public void testDeleteCard() throws Exception {
        // prepare vocabulary
        Vocabulary vocabularyMock = createVocabularyMock();
        expect(vocabularyMock.getWordsTotal()).andReturn(1); // override
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        replayTxCommited();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getId()).andStubReturn(1);
        replay(cardMock);
        addCardToCache(1, cardMock);

        cardDaoMock.delete(JOHN_DOE, 1);
        expectLastCall().once();
        replay(cardDaoMock);

        boolean done = sut.deleteCard(JOHN_DOE, 1);
        assertTrue(done);

        verify(cardMock);
        verify(cardDaoMock);
    }

    @Test
    public void getScoreSummary() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        addVocabularyToCache(vocabularyMock);
        replay(vocabularyMock);

        expect(cardDaoMock.getScoreSummary(JOHN_DOE, VOCAB_ID)).andReturn(Map.of("DEFERRED", 1, "LEARNT", 3));
        replay(cardDaoMock);

        Score score = sut.getScoreSummary(JOHN_DOE, VOCAB_ID);
        assertEquals(0, score.getToLearnCnt());
        assertEquals(1, score.getDeferredCnt());
        assertEquals(3, score.getLearntCnt());
        assertEquals(4, score.getTotalCount());
        assertNotNull(score);

        verify(cardDaoMock);
    }

    @Test
    public void testResetScore() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getId()).andStubReturn(1);
        replay(cardMock);
        addCardToCache(1, cardMock);

        cardDaoMock.updateStatus(1, CardStatus.TO_LEARN);
        expectLastCall().andReturn(1).once();

        cardDaoMock.updateScore(1, 0);
        expectLastCall().andReturn(1).once();

        replay(cardDaoMock);

        boolean done = sut.resetScore(JOHN_DOE, 1);
        assertTrue(done);

        verify(cardDaoMock);
    }

    @Test
    public void testResetScoreWhenThrowDaoException() throws Exception {
        replayTxRollback();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getId()).andReturn(1).anyTimes();
        expect(cardMock.getScore()).andReturn(0);
        expect(cardMock.getStatus()).andReturn(CardStatus.TO_LEARN);
        cardMock.setScore(0);
        replay(cardMock);

        addCardToCache(1, cardMock);

        cardDaoMock.updateStatus(1, CardStatus.DEFAULT_STATUS);
        expectLastCall().andReturn(1);
        cardDaoMock.updateScore(1, 0);
        expectLastCall().andStubThrow(new DaoException("resetScore", null));
        replay(cardDaoMock);

        boolean done = sut.resetScore(JOHN_DOE, 1);
        assertFalse(done);

        verify(cardDaoMock);
    }

    @Test
    public void testIncreaseScoreUpAndOneReachesFinalScore() throws Exception {
        replayTxCommited();
        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

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
        cardDaoMock.batchUpdateStatuses(List.of(cardMock2));
        expectLastCall().once();
        replay(cardDaoMock);

        int[] cardIdsSubmit = {1, 2, 2, 2, 1};
        boolean done = sut.increaseScoreUp(JOHN_DOE, VOCAB_ID, cardIdsSubmit, 10);
        assertTrue(done);

        verify(cardMock1, cardDaoMock);
    }

    @Test
    public void testGenerateCards() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        Word wordMock42 = strictMock(Word.class);
        expect(wordMock42.getId()).andReturn(42).times(2);
        expect(wordMock42.getValue()).andReturn("singleton").anyTimes();
        replay(wordMock42);

        Word wordMock87 = strictMock(Word.class);
        expect(wordMock87.getId()).andReturn(87).times(2);
        expect(wordMock87.getValue()).andReturn("generated").anyTimes();
        replay(wordMock87);

        Set<Integer> wordIds = Set.of(42, 87);
        cardDaoMock.addCards(eq(JOHN_DOE), anyInt(), eq(wordIds));
        expectLastCall().once();
        expect(wordDaoMock.selectAll(wordIds))
                .andReturn(List.of(wordMock42, wordMock87));
        expectLastCall().once();
        cardDaoMock.selectCards(JOHN_DOE, VOCAB_ID);
        Card card98 = new Card();
        card98.setId(98);
        card98.setVocabId(VOCAB_ID);
        card98.setWordId(42);
        Card card99 = new Card();
        card99.setId(99);
        card99.setVocabId(VOCAB_ID);
        card99.setWordId(87);
        expectLastCall().andReturn(List.of(card98, card99)).once();
        replay(wordDaoMock, cardDaoMock);

        List<Card> done = sut.generateCards(JOHN_DOE, VOCAB_ID, wordIds);
        assertFalse(done.isEmpty());

        verify(wordDaoMock, cardDaoMock);
    }

    private void addVocabularyToCache(Vocabulary vocabularyMock) {
        @SuppressWarnings("unchecked")
        var vocabsCache = (Map<OwnerId, List<Vocabulary>>) ReflectionTestUtils.getField(sut, "userVocabsCache");
        List<Vocabulary> vocabularies = new ArrayList<>();
        vocabularies.add(vocabularyMock);
        Objects.requireNonNull(vocabsCache).put(JOHN_DOE, vocabularies);
    }

    private void addCardToCache(int id, Card cardMock) {
        @SuppressWarnings("unchecked")
        var cardsCache = (Map<Integer, Card>) ReflectionTestUtils.getField(sut, "cardsCache");
        Objects.requireNonNull(cardsCache).put(id, cardMock);
    }

    private Vocabulary createVocabularyMock() {
        Vocabulary vocabularyMock = mock(Vocabulary.class);
        expect(vocabularyMock.getVocabId()).andReturn(VOCAB_ID).anyTimes();
        expect(vocabularyMock.getName()).andReturn(VOCAB_NAME);
        return vocabularyMock;
    }

    private void replayTxShouldNotStart() {
        replay(connectionMock);
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

}