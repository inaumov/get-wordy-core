package get.wordy.dao;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.bean.Word;
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
        wordDao = factory.getWordDao();
        assertNotNull(wordDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Word word = new Word(0, "apple", null, "transcription", null);

        Word inserted = wordDao.insert(word);
        assertTrue(inserted.getId() >= EXPECTED_NEW_ID);

        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        int id = 1;
        Iterator<Word> iterator = words.iterator();
        while (iterator.hasNext()) {
            Word actual = iterator.next();
            if (actual.getId() >= EXPECTED_NEW_ID) {
                assertEquals(word.getValue(), actual.getValue());
                assertEquals("transcription", actual.getTranscription());
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
    public void testDelete() throws DaoException {
        List<Word> wordsAll = wordDao.selectAll();
        assertNotNull(wordsAll);
        int cnt = wordsAll.size();
        assertEquals(PREDEFINED_WORDS_CNT, cnt);

        for (int i = 0, id = 1; i < PREDEFINED_WORDS_CNT; i++, id++) {
            Word toRemove = wordsAll.get(i);
            wordDao.delete(toRemove);
            List<Word> wordsAfter = wordDao.selectAll();
            assertNotNull(wordsAfter);
            assertEquals(--cnt, wordsAfter.size());
            assertTestData(wordsAfter, id + 1);
        }
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
        Set<String> strings = Collections.singleton("generated");
        Set<Integer> generated = wordDao.generate(strings);
        assertEquals(1, generated.size());

        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        LinkedList<Word> newList = new LinkedList<>(words);
        Word last = newList.getLast();
        assertTrue(last.getId() >= EXPECTED_NEW_ID);
        assertEquals("generated", last.getValue());
        assertNull(last.getTranscription());
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

}