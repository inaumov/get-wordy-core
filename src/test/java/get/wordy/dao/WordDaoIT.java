package get.wordy.dao;

import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.IWordDao;
import get.wordy.core.bean.Word;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

public class WordDaoIT extends DaoTestBase {

    private static final int PREDEFINED_WORDS_CNT = 5;
    private static final int EXPECTED_NEW_ID = 6;

    private IWordDao wordDao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        wordDao = factory.getWordDao();
        assertNotNull(wordDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Word word = new Word();
        word.setValue("word");
        word.setTranscription("transcription");

        wordDao.insert(word);
        assertEquals(EXPECTED_NEW_ID, word.getId());

        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        int id = 1;
        Iterator<Word> iterator = words.iterator();
        while (iterator.hasNext()) {
            Word actual = iterator.next();
            if (actual.getId() == EXPECTED_NEW_ID) {
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
            Word word = new Word();
            word.setId(id);
            word.setValue("word" + id);
            word.setTranscription("transcription" + id);
            wordDao.update(word);
        }
        // count words after updating
        List<Word> words = wordDao.selectAll();
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());

        int id = 1;
        Iterator<Word> iterator = words.iterator();
        while (iterator.hasNext()) {
            Word actual = iterator.next();
            assertEquals(id, actual.getId());
            assertEquals("word" + id, actual.getValue());
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

    private static void assertTestData(List<Word> words, int startFromId) {
        for (int i = 0, id = startFromId; i < words.size(); i++, id++) {
            Word next = words.get(i);
            assertEquals(id, next.getId());
            assertEquals("example" + id, next.getValue());
            assertNotNull(next.getTranscription());
            assertEquals(9, next.getTranscription().length());
        }
    }

}