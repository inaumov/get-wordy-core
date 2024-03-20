package get.wordy.dao;

import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.DictionaryDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_DICTIONARIES_CNT = 2;
    private static final int EXPECTED_NEW_ID = 3;
    private static final String LOGO_PNG = "http://logo.png";

    private DictionaryDao dictionaryDao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dictionaryDao = daoFactory.getDictionaryDao();
        assertNotNull(dictionaryDao);
    }

    @Test
    void testGetDictionary() throws DaoException {
        Dictionary dictionary = dictionaryDao.selectById(1);
        assertNotNull(dictionary);
        assertEquals(1, dictionary.getId());
        assertEquals("dictionary1", dictionary.getName());
        assertEquals(1, dictionary.getCardsTotal());
    }

    @Test
    void testGetDictionaryNotExists() throws DaoException {
        Dictionary dictionary = dictionaryDao.selectById(100);
        assertNull(dictionary);
    }

    @Test
    public void testInsert() throws DaoException {
        Dictionary dictionary = new Dictionary();
        dictionary.setName("name");
        dictionary.setPicture(LOGO_PNG);

        dictionaryDao.insert(dictionary);
        assertTrue(dictionary.getId() >= EXPECTED_NEW_ID);

        List<Dictionary> dictionaries = dictionaryDao.selectAll();
        assertNotNull(dictionaries);
        assertEquals(PREDEFINED_DICTIONARIES_CNT + 1, dictionaries.size());

        int id = 1;
        for (Dictionary actual : dictionaries) {
            if (actual.getId() >= EXPECTED_NEW_ID) {
                assertEquals(dictionary.getName(), actual.getName());
                assertEquals(LOGO_PNG, actual.getPicture());
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
    public void testUpdateName() throws DaoException {
        // update an existed dictionary
        Dictionary dictionary = new Dictionary(1, "name_changed_test", LOGO_PNG);
        dictionaryDao.update(dictionary);
        // verify after
        Dictionary actual = dictionaryDao.selectById(1);
        assertNotNull(actual);
        assertEquals(1, actual.getId());
        assertEquals("name_changed_test", actual.getName());
        assertNull(actual.getPicture());
    }

    @Test
    public void testUpdatePictureUrl() throws DaoException {
        // update an existed dictionary
        Dictionary dictionary = new Dictionary(1, null, LOGO_PNG);
        dictionaryDao.update(dictionary);
        // verify after
        Dictionary actual = dictionaryDao.selectById(1);
        assertNotNull(actual);
        assertEquals(1, actual.getId());
        assertEquals("dictionary1", actual.getName());
        assertEquals(LOGO_PNG, actual.getPicture());
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
            assertTrue(next.getCardsTotal() > 0);
        }
    }

}