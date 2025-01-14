package get.wordy.dao.impl;

import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.DictionaryDao;
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

@SpringJUnitConfig(classes = {DictionaryDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:test-data.sql") // load predefined inserts
public class DictionaryDaoTest {

    private static final String LOGO_PNG = "http://logo.png";

    @Autowired
    private DictionaryDao dictionaryDao;

    @Test
    public void testSelectAllByClassId() {
        OwnerId classOwner = new OwnerId("class001", "class");

        List<Dictionary> results = dictionaryDao.selectAllByOwnerId(classOwner);
        assertEquals(2, results.size());

        Dictionary firstVocab = results.stream()
                .filter(x -> x.getVocabId() == 101)
                .findFirst().orElseThrow();
        assertEquals("Vocabulary Basics", firstVocab.getName());
        assertEquals(4, firstVocab.getWordsTotal()); // Assume 4 words in 1st vocab

        Dictionary secondVocab = results.stream()
                .filter(x -> x.getVocabId() == 102)
                .findFirst().orElseThrow();
        assertEquals("Grammar 101", secondVocab.getName());
        assertEquals(7, secondVocab.getWordsTotal()); // Assume 6 words in 2nd vocab
    }

    @Test
    public void testInsert() {
        OwnerId classOwner = new OwnerId("class003", "class");
        Dictionary dictionary = new Dictionary("New Vocabulary", LOGO_PNG);

        // Insert new vocabulary
        Dictionary inserted = dictionaryDao.insert(classOwner, dictionary);
        assertNotNull(inserted);
        assertEquals("New Vocabulary", inserted.getName());
        assertEquals(LOGO_PNG, inserted.getPictureUrl());
        assertFalse(inserted.isShared());
        assertEquals(0, inserted.getWordsTotal());

        // Verify presence in DB
        List<Dictionary> results = dictionaryDao.selectAllByOwnerId(classOwner);
        assertEquals(1, results.size());
        assertEquals("New Vocabulary", results.getFirst().getName());
    }

    @Test
    public void testRename() {
        // rename vocabulary and verify
        int updated = dictionaryDao.rename(101, "Updated Vocabulary Basics");
        assertEquals(1, updated);

        // verify in DB
        Dictionary result = dictionaryDao.selectById(101)
                .orElseThrow();
        assertEquals("Updated Vocabulary Basics", result.getName());
    }

    @Test
    public void testUpdatePictureUrl() throws DaoException {
        // update an existed vocabulary
        int updated = dictionaryDao.updatePicture(1, LOGO_PNG);
        assertEquals(1, updated);

        // verify after
        Dictionary actual = dictionaryDao.selectById(1)
                .orElseThrow();
        assertNotNull(actual);
        assertEquals(1, actual.getVocabId());
        assertEquals("dictionary1", actual.getName());
        assertEquals(LOGO_PNG, actual.getPictureUrl());
    }

    @Test
    public void testSetIsShared() {
        // update sharing status and verify
        int updated = dictionaryDao.updateIsShared(101, true);
        assertEquals(1, updated);

        // Verify in DB
        Dictionary result = dictionaryDao.selectById(101)
                .orElseThrow();
        assertTrue(result.isShared());
    }

    @Test
    public void testSelectById() {
        // assume vocabulary id 101 exists in test-data.sql
        Optional<Dictionary> vocabulary = dictionaryDao.selectById(101);

        assertTrue(vocabulary.isPresent());
        Dictionary entity = vocabulary.get();
        assertEquals(101, entity.getVocabId());
        assertEquals("Vocabulary Basics", entity.getName());
        assertFalse(entity.isShared());
        assertEquals(4, entity.getWordsTotal()); // assume 10 words for id 101 in test-data.sql
    }

    @Test
    public void testSelectByIdNotFound() {
        Optional<Dictionary> vocabulary = dictionaryDao.selectById(100500);

        assertTrue(vocabulary.isEmpty());
    }

    @Test
    public void testDeleteVocabularyById() {
        int deleted = dictionaryDao.deleteVocabularyById(101);
        assertEquals(1, deleted);

        // Verify vocabulary is deleted
        Optional<Dictionary> deletedVocabulary = dictionaryDao.selectById(101);
        assertTrue(deletedVocabulary.isEmpty());

        // Verify associated word references are deleted
        Set<Integer> wordsRefs = dictionaryDao.getWordRefs(101);
        assertTrue(wordsRefs.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideIdsAdd")
    public void testAddRefsToVocabulary(int vocabId, Set<Integer> toAdd, int expectedTotal) {
        dictionaryDao.addWordsToVocabulary(vocabId, toAdd);

        Set<Integer> wordsRefs = dictionaryDao.getWordRefs(vocabId);
        assertEquals(expectedTotal, wordsRefs.size());
        // verify added references
        assertTrue(wordsRefs.containsAll(toAdd));
    }

    @Test
    public void testRemoveRefsFromVocabulary() {
        dictionaryDao.removeWordsFromVocabulary(101, Set.of(10, 13));

        Set<Integer> wordsRefs = dictionaryDao.getWordRefs(101);
        assertEquals(Set.of(11, 12), wordsRefs);
    }

    @ParameterizedTest
    @MethodSource(value = "provideIdsGet")
    public void testGetRefsByVocabularyId(int vocabId, Set<Integer> expected) {
        Set<Integer> wordsRefs = dictionaryDao.getWordRefs(vocabId);

        assertNotNull(wordsRefs);
        assertEquals(expected, wordsRefs);
    }

    private static Stream<Arguments> provideIdsGet() {
        return Stream.of(
                Arguments.of(101, Set.of(10, 11, 12, 13)),
                Arguments.of(102, Set.of(14, 15, 16, 17, 18, 19, 20)),
                Arguments.of(103, Set.of())
        );
    }

    private static Stream<Arguments> provideIdsAdd() {
        return Stream.of(
                Arguments.of(101, Set.of(14, 15, 16, 17, 18, 19, 20), 11),
                Arguments.of(102, Set.of(10, 11, 12, 13), 11),
                Arguments.of(103, Set.of(10, 20), 2)
        );
    }

}