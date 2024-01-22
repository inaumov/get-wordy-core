package get.wordy.dao;

import get.wordy.core.bean.Dictionary;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.DictionaryDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_DICTIONARIES_CNT = 2;
    private static final int EXPECTED_NEW_ID = 3;

    private DictionaryDao dictionaryDao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dictionaryDao = factory.getDictionaryDao();
        assertNotNull(dictionaryDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Dictionary dictionary = new Dictionary();
        dictionary.setName("name");

        dictionaryDao.insert(dictionary);
        assertTrue(dictionary.getId() >= EXPECTED_NEW_ID);

        List<Dictionary> dictionaries = dictionaryDao.selectAll();
        assertNotNull(dictionaries);
        assertEquals(PREDEFINED_DICTIONARIES_CNT + 1, dictionaries.size());

        int id = 1;
        for (Dictionary actual : dictionaries) {
            if (actual.getId() >= EXPECTED_NEW_ID) {
                assertEquals(dictionary.getName(), actual.getName());
            } else {
                assertEquals(id, actual.getId());
                assertEquals("dictionary" + id, actual.getName());
            }
            id++;
        }

        int count = dictionaryDao.count();
        assertEquals(PREDEFINED_DICTIONARIES_CNT + 1, count);
    }

    @Test
    public void testUpdate() throws DaoException {
        // update an existed dictionary
        for (int id = 1; id <= PREDEFINED_DICTIONARIES_CNT; id++) {
            Dictionary word = new Dictionary(id, "name" + id);
            dictionaryDao.update(word);
        }
        // count dictionaries after updating
        List<Dictionary> dictionaries = dictionaryDao.selectAll();
        assertNotNull(dictionaries);
        assertEquals(PREDEFINED_DICTIONARIES_CNT, dictionaries.size());

        int id = 1;
        for (Dictionary actual : dictionaries) {
            assertEquals(id, actual.getId());
            assertEquals("name" + id, actual.getName());
            id++;
        }
        int count = dictionaryDao.count();
        assertEquals(PREDEFINED_DICTIONARIES_CNT, count);
    }

    @Test
    public void testDelete() throws DaoException {
        List<Dictionary> dictionariesAll = dictionaryDao.selectAll();
        assertNotNull(dictionariesAll);
        int cnt = dictionariesAll.size();
        assertEquals(PREDEFINED_DICTIONARIES_CNT, cnt);

        for (int i = 0, id = 1; i < PREDEFINED_DICTIONARIES_CNT; i++, id++) {
            Dictionary toRemove = dictionariesAll.get(i);
            dictionaryDao.delete(toRemove);
            List<Dictionary> dictionariesAfter = dictionaryDao.selectAll();
            assertNotNull(dictionariesAfter);
            assertEquals(--cnt, dictionariesAfter.size());
            assertTestData(dictionariesAfter, id + 1);
        }
        assertEquals(0, dictionaryDao.count());
    }

    @Test
    public void testSelectAll() throws DaoException {
        List<Dictionary> dictionaries = dictionaryDao.selectAll();
        assertNotNull(dictionaries);
        assertEquals(PREDEFINED_DICTIONARIES_CNT, dictionaries.size());
        assertTestData(dictionaries, 1);
    }

    @Test
    public void testCount() throws DaoException {
        int count = dictionaryDao.count();
        assertEquals(PREDEFINED_DICTIONARIES_CNT, count);
    }

    private static void assertTestData(List<Dictionary> dictionaries, int startFromId) {
        for (int i = 0, id = startFromId; i < dictionaries.size(); i++, id++) {
            Dictionary next = dictionaries.get(i);
            assertEquals(id, next.getId());
            assertEquals("dictionary" + id, next.getName());
        }
    }

}