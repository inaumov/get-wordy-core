package get.wordy.core;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Word;
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

        boolean done = dictionaryService.createDictionary(DICTIONARY_NAME) != null;
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

        boolean exceptionHappened = dictionaryService.createDictionary(DICTIONARY_NAME) == null;
        assertTrue(exceptionHappened);
        assertEquals(DICTIONARY_NAME, dictionaryCapture.getValue().getName());
        verify(connectionMock, dictionaryDaoMock);
    }

    @Test
    public void testRenameDictionary() throws DaoException {
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
        boolean done = dictionaryService.renameDictionary(DICTIONARY_ID, "nameUpdated");
        assertFalse(done);
    }

    @Test
    public void testRenameDictionaryWhenDaoException() throws DaoException {
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
    public void testRemoveDictionary() throws DaoException {
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
        boolean done = dictionaryService.removeDictionary(DICTIONARY_ID);
        assertFalse(done);
    }

    @Test
    public void testRemoveDictionaryWhenDaoException() throws DaoException {
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
    public void testGetCards() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);
        Word wordMock = createWordMock(1, "word");
        replay(wordMock);
        Card cardMock = createCardMock(1);
        cardMock.setWord(wordMock);
        replay(cardMock);

        expect(cardDaoMock.selectCardsForDictionary(anyObject(Dictionary.class)))
                .andReturn(Collections.singletonList(cardMock));
        expectLastCall().once();
        replay(cardDaoMock);

        expect(wordDaoMock.selectAll())
                .andReturn(Collections.singletonList(wordMock));
        expectLastCall().once();
        replay(wordDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        Map<String, Card> cards = dictionaryService.getCards(DICTIONARY_ID);
        assertNotNull(cards);
        assertFalse(cards.isEmpty());
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testGetCardsForExercise() throws DaoException {
        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        int[] ids = {8, 1, 3};
        expect(cardDaoMock.selectCardIdsForExercise(anyInt(), anyInt()))
                .andReturn(ids)
                .once();

        int[] all = {1, 2, 3, 5, 8, 13};
        for (int id : all) {
            Card cardMock = createCardMock(id);
            Word wordMock = createWordMock(id, "word " + id);
            replay(wordMock);
            cardMock.setWord(wordMock);
            expect(cardMock.getWord()).andStubReturn(wordMock);
            replay(cardMock);
            addCardToCache("word " + id, cardMock);
        }

        replay(cardDaoMock);

        List<Card> cards = dictionaryService.getCardsForExercise(1, 6);
        assertEquals(3, cards.size());
    }

    @Test
    public void testSaveNewCard() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = createWordMock(1, "test");
        expect(wordMock.id()).andReturn(1).times(2);
        replay(wordMock);

        Card cardMock = createCardMock(0);
        expect(cardMock.getId()).andStubReturn(1);
        expect(cardMock.getWord()).andStubReturn(wordMock);
        cardMock.setWordId(anyInt());
        cardMock.setInsertedAt(anyObject(Instant.class));
        replay(cardMock);
        addCardToCache("test", cardMock);

        wordDaoMock.insert(wordMock);
        expectLastCall().andAnswer(() -> wordMock);
        cardDaoMock.insert(cardMock);
        expectLastCall().andAnswer(() -> cardMock);
        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.save(cardMock);
        assertTrue(done);
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testUpdateCard() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        replay(dictionaryMock);
        addDictionaryToCache(dictionaryMock);

        Word wordMock = createWordMock(1, "test");
        expect(wordMock.id()).andReturn(1).times(2);
        replay(wordMock);

        Card cardMock = createCardMock(1);
        expect(cardMock.getId()).andStubReturn(1);
        expect(cardMock.getWord()).andStubReturn(wordMock);
        cardMock.setWordId(anyInt());
        cardMock.setInsertedAt(anyObject(Instant.class));
        replay(cardMock);
        addCardToCache("test", cardMock);

        Word wordForUpdMock = createNiceMock(Word.class);
        expect(wordForUpdMock.id()).andStubReturn(2);
        replay(wordForUpdMock);

        Card cardForUpdMock = createNiceMock(Card.class);
        expect(cardForUpdMock.getId()).andStubReturn(1);
        expect(cardForUpdMock.getWord()).andReturn(wordForUpdMock);
        replay(cardForUpdMock);

        wordDaoMock.update(wordForUpdMock);
        expectLastCall().andAnswer(() -> wordForUpdMock);
        cardDaoMock.update(cardForUpdMock);
        expectLastCall().andAnswer(() -> cardForUpdMock);
        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.save(cardForUpdMock);

        assertTrue(done);
        verify(cardForUpdMock, wordForUpdMock);
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testRemoveCard() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = createMock(Card.class);
        expect(cardMock.getWordId()).andStubReturn(1);
        Word wordMock = createMock(Word.class);
        expect(wordMock.value()).andReturn("word");
        expect(cardMock.getWord()).andStubReturn(wordMock);
        replay(cardMock, wordMock);
        addCardToCache("word", cardMock);

        cardDaoMock.delete(cardMock);
        expectLastCall().once();
        replay(cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);


        boolean done = dictionaryService.removeCard("word");
        assertTrue(done);
        verify(cardMock, wordMock);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testChangeStatusWhenThrowDaoException() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = createMock(Card.class);
        cardMock.setStatus(CardStatus.DEFAULT_STATUS);
        cardMock.setRating(0);
        replay(cardMock);

        addCardToCache("word", cardMock);

        cardDaoMock.updateStatus(cardMock);
        expectLastCall().andStubThrow(new DaoException("changeStatus", null));
        replay(cardDaoMock);

        connectionMock.rollback();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.changeStatus("word", CardStatus.DEFAULT_STATUS);
        assertFalse(done);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @ParameterizedTest
    @CsvSource(value = {"EDIT,0", "TO_LEARN,50", "LEARNT,100", "POSTPONED,20"})
    public void testChangeStatus(CardStatus cardStatus, int rating) throws DaoException {
        Card cardMock = createMock(Card.class);
        cardMock.setStatus(cardStatus);
        cardMock.setRating(rating);
        replay(cardMock);
        addCardToCache("word", cardMock);
        doChangeStatusTest("word", cardMock, cardStatus);
    }

    private void doChangeStatusTest(String word, Card cardMock, CardStatus status) throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        cardDaoMock.updateStatus(cardMock);
        expectLastCall().andReturn(cardMock);
        replay(cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.changeStatus(word, status);
        assertTrue(done);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testGetScore() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Dictionary dictionaryMock = createDictionaryMock();
        addDictionaryToCache(dictionaryMock);

        expect(cardDaoMock.getScore(anyObject(Dictionary.class))).andReturn(Map.of("EDIT", 1));
        replay(cardDaoMock);

        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        Score score = dictionaryService.getScore(DICTIONARY_ID);
        assertEquals(1, score.getTotalCount());
        assertEquals(1, score.getEditCnt());
        assertEquals(0, score.getToLearnCnt());
        assertEquals(0, score.getPostponedCnt());
        assertEquals(0, score.getLearntCnt());
        assertNotNull(score);
        verify(cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testResetScore() throws DaoException {
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
    public void testIncreaseScoreUp() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(10).once();
        cardMock.setRating(20);
        expectLastCall().once();
        replay(cardMock);
        addCardToCache("word", cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().andReturn(cardMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp("word", 10);
        assertTrue(done);
        verify(cardMock, cardDaoMock);
        verify(connectionMock);
    }

    @Test
    public void testIncreaseScoreUpWhenReach100Percents() throws DaoException {
        connectionMock.open();
        expectLastCall().once();

        Card cardMock = createStrictMock(Card.class);
        expect(cardMock.getRating()).andReturn(90).once();
        cardMock.setStatus(CardStatus.LEARNT);
        expectLastCall().once();
        cardMock.setRating(100);
        expectLastCall().once();
        replay(cardMock);
        addCardToCache("word", cardMock);

        cardDaoMock.update(cardMock);
        expectLastCall().andReturn(cardMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        replay(cardDaoMock);

        boolean done = dictionaryService.increaseScoreUp("word", 10);
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

        Set<String> words = Collections.singleton("generated");
        Set<Integer> wordIds = Collections.singleton(87);
        expect(wordDaoMock.generate(words)).andReturn(wordIds).once();
        expect(cardDaoMock.generateCardsWithoutDefinitions(anyObject(Set.class), anyInt(), anyObject(CardStatus.class)))
                .andReturn(true).once();

        replay(wordDaoMock, cardDaoMock);

        connectionMock.commit();
        expectLastCall().once();
        connectionMock.close();
        expectLastCall().once();
        replay(connectionMock);

        boolean done = dictionaryService.generateCards(DICTIONARY_ID, words);
        assertTrue(done);
        verify(wordDaoMock, cardDaoMock);
        verify(connectionMock);
    }

    private void addDictionaryToCache(Dictionary dictionaryMock) {
        dictionaryService.getDictionaryCache().add(dictionaryMock);
    }

    private void addCardToCache(String word, Card cardMock) {
        dictionaryService.getCardsCache().put(word, cardMock);
    }

    private Dictionary createDictionaryMock() {
        Dictionary dictionaryMock = createNiceMock(Dictionary.class);
        expect(dictionaryMock.getId()).andReturn(DICTIONARY_ID).atLeastOnce();
        expect(dictionaryMock.getName()).andReturn(DICTIONARY_NAME).atLeastOnce();
        return dictionaryMock;
    }

    private Word createWordMock(int id, String word) {
        Word wordMock = createNiceMock(Word.class);
        expect(wordMock.id()).andReturn(id);
        expect(wordMock.value()).andReturn(word);
        return wordMock;
    }

    private Card createCardMock(int id) {
        Card cardMock = createNiceMock(Card.class);
        expect(cardMock.getId()).andReturn(id);
        expect(cardMock.getDictionaryId()).andReturn(1);
        expect(cardMock.getWordId()).andReturn(id);
        expect(cardMock.getRating()).andReturn(90);
        return cardMock;
    }

}