package get.wordy.dao.impl;

import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.id.OwnerId;
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
        OwnerId user = new OwnerId("john-123", "user");

        Dictionary dictionary = new Dictionary();
        dictionary.setName("name");
        dictionary.setPicture(LOGO_PNG);

        dictionaryDao.insert(dictionary);
        dictionaryDao.updateOwnerRelation(user, dictionary);

        assertTrue(dictionary.getId() >= EXPECTED_NEW_ID);

        List<Dictionary> dictionaries = dictionaryDao.selectAllByOwnerId(user);
        assertNotNull(dictionaries);
        assertEquals(2, dictionaries.size());

        Dictionary last = dictionaries.getLast();
        assertEquals(LOGO_PNG, last.getPicture());
        assertEquals("name", last.getName());

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
        dictionaryDao.delete(1);
        List<Dictionary> dictionaries = dictionaryDao.selectAllByOwnerId(new OwnerId("john-123", "user"));
        assertNotNull(dictionaries);
        assertEquals(1, dictionaryDao.count());
        assertTestData(dictionaries, 2);
    }

    @Test
    public void testSelectAll() throws DaoException {
        List<Dictionary> dictionaries = dictionaryDao.selectAllByOwnerId(new OwnerId("john-123", "user"));
        assertNotNull(dictionaries);
        assertEquals(1, dictionaries.size());
        assertTestData(dictionaries, 1);
        dictionaries = dictionaryDao.selectAllByOwnerId(new OwnerId("class-42", "class"));
        assertNotNull(dictionaries);
        assertEquals(1, dictionaries.size());
        assertTestData(dictionaries, 2);
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