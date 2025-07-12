package get.wordy.core;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.exception.VocabNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.ProgressDao;
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
    private ProgressDao progressDaoMock;
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

        vocabularyDaoMock.rename(JOHN_DOE, vocabularyMock.getVocabId(), "nameUpdated");
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
        expect(cardMock.getVocabId()).andReturn(1);
        expect(cardMock.getWordId()).andReturn(1);
        replay(cardMock);

        expect(headlineDaoMock.getCards(JOHN_DOE.ownerId(), VOCAB_ID))
                .andReturn(Collections.singletonList(cardMock));
        expectLastCall().once();
        replay(headlineDaoMock);

        List<Card> cards = sut.getCards(JOHN_DOE, VOCAB_ID);
        assertNotNull(cards);
        assertEquals(1, cards.size());

        verify(headlineDaoMock);
    }

    @Test
    public void testGetCardsForExercise_FromDb() throws Exception {
        replayTxCommited();

        int[] wordIds = {2, 5};
        List<Exercise> fromDb = Arrays.stream(wordIds)
                .mapToObj(wordId -> {
                    Exercise exercise = strictMock(Exercise.class);
                    expect(exercise.getWordId()).andReturn(wordId);
                    return exercise;
                }).toList();
        addExerciseToCache(VOCAB_ID + ":" + JOHN_DOE.ownerId(), List.of(fromDb.getFirst()));

        expect(headlineDaoMock.getCardsForExercise(JOHN_DOE.ownerId(), VOCAB_ID, 5))
                .andReturn(fromDb);
        expectLastCall().once();
        replay(headlineDaoMock);

        List<Exercise> result = sut.getCardsForExercise(JOHN_DOE, VOCAB_ID, 5);
        assertEquals(2, result.size());

        verify(headlineDaoMock); // no interaction
    }

    @Test
    public void testGetCardsForExercise_FromCache() throws Exception {
        replayTxCommited();

        int[] wordIds = {1, 2, 3, 5, 8, 13};
        List<Exercise> inCache = Arrays.stream(wordIds)
                .mapToObj(wordId -> {
                    int cardId = new Random().nextInt(101);
                    Word wordMock = niceMock(Word.class);
                    expect(wordMock.getId()).andReturn(wordId).anyTimes();
                    replay(wordMock);

                    Exercise exercise = niceMock(Exercise.class);
                    expect(exercise.getWordId()).andReturn(wordId).anyTimes();
                    expect(exercise.getCardId()).andReturn(cardId).anyTimes();
                    expect(exercise.getWord()).andReturn(wordMock).anyTimes();
                    expect(exercise.getSentences()).andReturn(List.of()).anyTimes();
                    replay(exercise);
                    return exercise;
                }).toList();
        addExerciseToCache(JOHN_DOE.ownerId() + ":" + VOCAB_ID, inCache);

        replay(headlineDaoMock);

        List<Exercise> cards = sut.getCardsForExercise(JOHN_DOE, VOCAB_ID, 5);
        assertEquals(5, cards.size());
        for (Exercise card : cards) {
            assertTrue(card.getWordId() > 0);
            assertNotNull(card.getWord());
            assertTrue(card.getSentences().isEmpty());
        }
        verify(headlineDaoMock); // no interaction
    }

    @Test
    void addToVocabulary() throws Exception {
        Word wordMock = niceMock(Word.class);
        replay(wordMock);

        replayTxCommited();
        expect(wordDaoMock.selectById(99)).andReturn(wordMock);
        replay(wordDaoMock);

        expect(vocabularyDaoMock.hasAccess(JOHN_DOE, VOCAB_ID))
                .andReturn(true);
        vocabularyDaoMock.addWordsToVocabulary(VOCAB_ID, 99);
        expectLastCall().once();
        replay(vocabularyDaoMock);

        sut.addToVocabulary(JOHN_DOE, VOCAB_ID, 99);

        verify(vocabularyDaoMock);
    }

    @Test
    public void testDeleteCard() throws Exception {

        Word wordMock = niceMock(Word.class);
        expect(wordMock.getId()).andReturn(99).anyTimes();
        replay(wordMock);
        addWordsToVocab(VOCAB_ID, wordMock);

        replayTxCommited();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andReturn(99);
        replay(cardMock);
        addCardToCache(JOHN_DOE, VOCAB_ID, cardMock);

        progressDaoMock.delete(JOHN_DOE, VOCAB_ID, 99);
        expectLastCall().once();
        replay(progressDaoMock);

        vocabularyDaoMock.removeWordsFromVocabulary(VOCAB_ID, 99);
        expectLastCall().once();
        replay(vocabularyDaoMock);

        sut.removeFromVocabulary(JOHN_DOE, VOCAB_ID, 99);

        verify(cardMock);
        verify(progressDaoMock);
    }

    @Test
    public void getProgressSummary() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        addVocabularyToCache(vocabularyMock);
        replay(vocabularyMock);

        expect(progressDaoMock.getProgressSummary(JOHN_DOE, VOCAB_ID)).andReturn(Map.of("DEFERRED", 1, "LEARNT", 3));
        replay(progressDaoMock);

        Score score = sut.getProgressSummary(JOHN_DOE, VOCAB_ID);
        assertEquals(0, score.getToLearnCnt());
        assertEquals(1, score.getDeferredCnt());
        assertEquals(3, score.getLearntCnt());
        assertEquals(4, score.getTotalCount());
        assertNotNull(score);

        verify(progressDaoMock);
    }

    @Test
    public void testResetProgress() throws Exception {
        replayTxCommited();

        Vocabulary vocabularyMock = createVocabularyMock();
        replay(vocabularyMock);
        addVocabularyToCache(vocabularyMock);

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andStubReturn(1);
        cardMock.setScore(0);
        cardMock.setStatus(CardStatus.TO_LEARN);
        replay(cardMock);
        addCardToCache(JOHN_DOE, VOCAB_ID, cardMock);

        progressDaoMock.updateProgress(JOHN_DOE, cardMock);
        expectLastCall().andReturn(1).once();

        replay(progressDaoMock);

        boolean done = sut.resetProgress(JOHN_DOE, VOCAB_ID, 1);
        assertTrue(done);

        verify(progressDaoMock);
    }

    @Test
    public void testResetProgressWhenThrowDaoException() throws Exception {
        replayTxRollback();

        Card cardMock = strictMock(Card.class);
        expect(cardMock.getWordId()).andReturn(1).anyTimes();
        cardMock.setScore(0);
        cardMock.setStatus(CardStatus.TO_LEARN);
        replay(cardMock);

        addCardToCache(JOHN_DOE, VOCAB_ID, cardMock);

        progressDaoMock.updateProgress(JOHN_DOE, cardMock);
        expectLastCall().andStubThrow(new DaoException("resetScore", null));
        replay(progressDaoMock);

        boolean done = sut.resetProgress(JOHN_DOE, VOCAB_ID, 1);
        assertFalse(done);

        verify(progressDaoMock);
    }

    @Test
    public void testUpdateProgress_whenCardsExisting() throws Exception {
        replayTxCommited();

        Card cardMock1 = strictMock(Card.class);
        expect(cardMock1.getWordId()).andReturn(1);
        expect(cardMock1.getStatus()).andReturn(CardStatus.TO_LEARN);
        expect(cardMock1.getScore()).andReturn(10).once();
        cardMock1.setScore(20);
        expectLastCall().once();
        //
        Card cardMock2 = strictMock(Card.class);
        expect(cardMock2.getWordId()).andReturn(2);
        expect(cardMock2.getStatus()).andReturn(CardStatus.TO_LEARN);
        expect(cardMock2.getScore()).andReturn(95).once();
        cardMock2.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock2.setScore(100);
        expectLastCall().once();

        replay(cardMock1, cardMock2);
        addCardToCache(JOHN_DOE, VOCAB_ID, cardMock1, cardMock2);

        progressDaoMock.selectCards(JOHN_DOE, VOCAB_ID, 1, 2);
        expectLastCall().andReturn(List.of(cardMock1, cardMock2)).once();

        progressDaoMock.batchUpsertProgress(JOHN_DOE, List.of(cardMock1, cardMock2));
        expectLastCall().once();
        replay(progressDaoMock);

        int[] idsSubmit = {1, 2, 2, 2, 1};
        sut.saveProgress(JOHN_DOE, VOCAB_ID, idsSubmit, 10);

        verify(cardMock1, cardMock2, progressDaoMock);
    }

    @Test
    public void testUpdateProgress_whenGenerateOneCard() throws Exception {
        replayTxCommited();

        Card insertedCardMock = strictMock(Card.class);
        expect(insertedCardMock.getWordId()).andReturn(5).anyTimes();
        expect(insertedCardMock.getStatus()).andReturn(CardStatus.TO_LEARN);
        expect(insertedCardMock.getScore()).andReturn(30);
        insertedCardMock.setScore(40);
        expectLastCall().once();
        replay(insertedCardMock);

        progressDaoMock.selectCards(JOHN_DOE, VOCAB_ID, 5);
        expectLastCall().andReturn(Collections.emptyList());

        Capture<Card> cardCapture = Capture.newInstance();
        progressDaoMock.insert(eq(JOHN_DOE), capture(cardCapture));
        expectLastCall().andAnswer(() -> insertedCardMock);

        progressDaoMock.batchUpsertProgress(JOHN_DOE, List.of(insertedCardMock));
        expectLastCall().once();
        replay(progressDaoMock);

        int[] idsSubmit = {5};
        sut.saveProgress(JOHN_DOE, VOCAB_ID, idsSubmit, 10);

        Assertions.assertEquals(VOCAB_ID, cardCapture.getValue().getVocabId());
        Assertions.assertEquals(5, cardCapture.getValue().getWordId());
        Assertions.assertEquals(CardStatus.TO_LEARN, cardCapture.getValue().getStatus());

        verify(progressDaoMock);
    }

    @Test
    public void testUpdateProgress_whenGenerateSeveralCards() throws Exception {
        replayTxCommited();

        Card card98 = new Card();
        card98.setVocabId(VOCAB_ID);
        card98.setWordId(42);
        card98.setStatus(CardStatus.TO_LEARN);
        //
        Card card99 = new Card();
        card99.setVocabId(VOCAB_ID);
        card99.setWordId(87);
        card99.setStatus(CardStatus.TO_LEARN);

        progressDaoMock.selectCards(JOHN_DOE, VOCAB_ID, 42, 87);
        expectLastCall().andReturn(Collections.emptyList());

        progressDaoMock.addCards(JOHN_DOE, List.of(card98, card99));
        expectLastCall().once();

        progressDaoMock.selectCards(JOHN_DOE, VOCAB_ID, 42, 87);
        expectLastCall().andReturn(List.of(card98, card99)).anyTimes();

        progressDaoMock.batchUpsertProgress(JOHN_DOE, List.of(card98, card99));
        expectLastCall().once();
        replay(progressDaoMock);

        int[] idsSubmit = {42, 87};
        sut.saveProgress(JOHN_DOE, VOCAB_ID, idsSubmit, 10);

        verify(progressDaoMock);
    }

    private void addVocabularyToCache(Vocabulary vocabularyMock) {
        @SuppressWarnings("unchecked")
        var vocabsCache = (Map<OwnerId, List<Vocabulary>>) ReflectionTestUtils.getField(sut, "userVocabsCache");
        List<Vocabulary> vocabularies = new ArrayList<>();
        vocabularies.add(vocabularyMock);
        Objects.requireNonNull(vocabsCache).put(JOHN_DOE, vocabularies);
    }

    private void addWordsToVocab(int vocabId, Word... wordMocks) {
        @SuppressWarnings("unchecked")
        var wordsCache = (Map<Integer, List<Word>>) ReflectionTestUtils.getField(sut, "wordsInVocabularyCache");
        List<Word> list = new ArrayList<>(Arrays.asList(wordMocks));
        Objects.requireNonNull(wordsCache).put(vocabId, list);
    }

    private void addCardToCache(OwnerId ownerId, int vocabId, Card... cardMocks) {
        @SuppressWarnings("unchecked")
        var cardsCache = (Map<String, List<Card>>) ReflectionTestUtils.getField(sut, "cardsCache");
        String key = String.format("%s:%d", ownerId.ownerId(), vocabId);
        List<Card> list = new ArrayList<>(Arrays.asList(cardMocks));
        Objects.requireNonNull(cardsCache).put(key, list);
    }

    private void addExerciseToCache(String key, List<Exercise> cardsMock) {
        @SuppressWarnings("unchecked")
        var exerciseCache = (Map<String, List<Exercise>>) ReflectionTestUtils.getField(sut, "exerciseCache");
        Objects.requireNonNull(exerciseCache).put(key, cardsMock);
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