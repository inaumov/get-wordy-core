package get.wordy.dao.impl;

import get.wordy.core.api.bean.WordsheetHeader;
import get.wordy.core.dao.impl.WordsheetDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {WordsheetDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:classes.sql") // load predefined inserts
public class WordsheetDaoTest {

    @Autowired
    private WordsheetDao wordsheetDao;

    @Test
    public void testSelectAllByClassId() {
        List<WordsheetHeader> results = wordsheetDao.selectAllByClassId("class001");
        assertEquals(2, results.size());

        WordsheetHeader firstSheet = results.getFirst();
        assertEquals("Vocabulary Basics", firstSheet.name());
        assertEquals(4, firstSheet.wordsTotal()); // Assume 4 words in 1st vocab

        WordsheetHeader secondSheet = results.get(1);
        assertEquals("Grammar 101", secondSheet.name());
        assertEquals(7, secondSheet.wordsTotal()); // Assume 6 words in 2nd vocab
    }

    @Test
    public void testInsert() {
        // insert and verify
        WordsheetHeader inserted = wordsheetDao.insert("class003", "New Wordsheet");
        assertNotNull(inserted);
        assertEquals("New Wordsheet", inserted.name());
        assertFalse(inserted.isShared());
        assertEquals(0, inserted.wordsTotal()); // newly inserted wordsheet has no words

        // verify in DB
        List<WordsheetHeader> results = wordsheetDao.selectAllByClassId("class003");
        assertEquals(1, results.size());
        assertEquals("New Wordsheet", results.getFirst().name());
    }

    @Test
    public void testRename() {
        // rename wordsheet and verify
        WordsheetHeader updated = wordsheetDao.rename(1, "Updated Vocabulary Basics");
        assertNotNull(updated);
        assertEquals("Updated Vocabulary Basics", updated.name());

        // verify in DB
        List<WordsheetHeader> results = wordsheetDao.selectAllByClassId("class001");
        assertEquals("Updated Vocabulary Basics", results.getFirst().name());
    }

    @Test
    public void testSetIsShared() {
        // update sharing status and verify
        WordsheetHeader updated = wordsheetDao.setIsShared(1, true);
        assertNotNull(updated);
        assertTrue(updated.isShared());

        // Verify in DB
        List<WordsheetHeader> results = wordsheetDao.selectAllByClassId("class001");
        assertTrue(results.getFirst().isShared());
    }

    @Test
    public void testSelectById() {
        // assume wordsheet id 1 exists in test-data.sql
        Optional<WordsheetHeader> wordsheet = wordsheetDao.selectById(1);

        assertTrue(wordsheet.isPresent());
        WordsheetHeader entity = wordsheet.get();
        assertEquals(1, entity.wordsheetId());
        assertEquals("Vocabulary Basics", entity.name());
        assertFalse(entity.isShared());
        assertEquals(4, entity.wordsTotal()); // assume 10 words for id 1 in test-data.sql
    }

    @Test
    public void testSelectByIdNotFound() {
        Optional<WordsheetHeader> wordsheet = wordsheetDao.selectById(100500);

        assertTrue(wordsheet.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideIdsAdd")
    public void testAddRefsToWordsheet(int wordsheetId, Set<Integer> toAdd, int total) {
        wordsheetDao.addToWordsheet(wordsheetId, toAdd);

        Set<Integer> wordsRefs = wordsheetDao.getWordsRefs(wordsheetId);
        assertEquals(total, wordsRefs.size());
    }

    @Test
    public void testRemoveRefsFromWordsheet() {
        wordsheetDao.removeFromWordsheet(1, Set.of(10, 13));

        Set<Integer> wordsRefs = wordsheetDao.getWordsRefs(1);
        assertEquals(Set.of(11, 12), wordsRefs);
    }

    @ParameterizedTest
    @MethodSource(value = "provideIdsGet")
    public void testGetRefsByWordsheetId(int wordsheetId, Set<Integer> expected) {
        Set<Integer> wordsRefs = wordsheetDao.getWordsRefs(wordsheetId);

        assertNotNull(wordsRefs);
        assertEquals(expected, wordsRefs);
    }

    private static Stream<Arguments> provideIdsGet() {
        return Stream.of(
                Arguments.of(1, Set.of(10, 11, 12, 13)),
                Arguments.of(2, Set.of(14, 15, 16, 17, 18, 19, 20)),
                Arguments.of(3, Set.of())
        );
    }

    private static Stream<Arguments> provideIdsAdd() {
        return Stream.of(
                Arguments.of(1, Set.of(14, 15, 16, 17, 18, 19, 20), 11),
                Arguments.of(2, Set.of(10, 11, 12, 13), 11),
                Arguments.of(3, Set.of(10, 20), 2)
        );
    }

}
