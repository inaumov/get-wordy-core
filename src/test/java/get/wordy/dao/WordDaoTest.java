package get.wordy.dao;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.impl.WordDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class WordDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_WORDS_CNT = 3;
    private static final int EXPECTED_NEW_ID = 4;

    private WordDao wordDao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        wordDao = daoFactory.getWordDao();
        assertNotNull(wordDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Word word = new Word(0, "apple", "noun", "transcription", "Some text");

        Word inserted = wordDao.insert(word);
        assertTrue(inserted.getId() >= EXPECTED_NEW_ID);

        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        int id = 1;
        for (Word actual : words) {
            if (actual.getId() >= EXPECTED_NEW_ID) {
                assertEquals(word.getValue(), actual.getValue());
                assertEquals("noun", actual.getPartOfSpeech());
                assertEquals("transcription", actual.getTranscription());
                assertEquals("Some text", actual.getMeaning());
            } else {
                assertEquals(id, actual.getId());
                assertEquals("example" + id, actual.getValue());
                assertNotNull(actual.getTranscription());
            }
            id++;
        }
    }

    @Test
    public void testUpdate() throws DaoException {
        // update an existed word
        for (int id = 1; id <= PREDEFINED_WORDS_CNT; id++) {
            Word word = new Word(id, "to test " + id, "VERB", "transcription" + id, "test");
            wordDao.update(word);
        }
        // count words after updating
        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());

        int id = 1;
        for (Word actual : words) {
            assertEquals(id, actual.getId());
            assertEquals("to test " + id, actual.getValue());
            assertEquals("transcription" + id, actual.getTranscription());
            id++;
        }
    }

    @Test
    public void testDeleteAbandonedWord() throws DaoException {
        int abandonedWordId = 3;
        Word toRemove = new Word();
        wordDao.delete(toRemove.withId(abandonedWordId));
        List<Word> wordsAfter = wordDao.selectAll();
        assertNotNull(wordsAfter);
        assertEquals(PREDEFINED_WORDS_CNT - 1, wordsAfter.size());
        assertTestData(wordsAfter, 1);
    }

    @Test
    public void testDeleteWordViolationException() throws DaoException {
        int wordIdReferenced = 1;
        Word toRemove = new Word();
        DaoException daoException = assertThrows(DaoException.class,
                () -> wordDao.delete(toRemove.withId(wordIdReferenced))
        );
        assertEquals("Error while deleting word entity", daoException.getMessage());
    }

    @Test
    public void testSelectAll() throws DaoException {
        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());
        assertTestData(words, 1);
    }

    @Test
    public void testGenerateWords() throws Exception {
        Set<String> strings = Set.of("singleton", "generated");
        Set<Integer> generated = wordDao.generate(strings);
        assertEquals(2, generated.size());

        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 2, words.size());

        // validate at least one word
        LinkedList<Word> newList = new LinkedList<>(words);
        Word last = newList.getLast();
        assertTrue(last.getId() >= EXPECTED_NEW_ID);
        assertTrue(strings.contains(last.getValue()));
        assertNull(last.getTranscription());
        assertNull(last.getMeaning());
    }

    private static void assertTestData(List<Word> words, int startFromId) {
        for (int i = 0, id = startFromId; i < words.size(); i++, id++) {
            Word next = words.get(i);
            assertEquals(id, next.getId());
            assertEquals("example" + id, next.getValue());
            assertNotNull(next.getTranscription());
            assertEquals("noun", next.getPartOfSpeech().toLowerCase());
            assertNotNull(next.getMeaning());
        }
    }

    @Test
    void testGetWord() throws DaoException {
        var word = wordDao.selectById(1);
        assertNotNull(word);
        assertEquals(1, word.getId());
        assertEquals("example1", word.getValue());
        assertTrue(word.getMeaning().contains("a word"));
        assertEquals("noun", word.getPartOfSpeech());
        assertNotNull(word.getTranscription());
    }

    @Test
    void testGetWordNotExists() throws DaoException {
        var word = wordDao.selectById(100);
        assertNull(word);
    }

}