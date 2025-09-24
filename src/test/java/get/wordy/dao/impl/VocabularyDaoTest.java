package get.wordy.dao.impl;

import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.exception.DuplicateVocabularyException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.VocabularyDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {VocabularyDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:test-data.sql") // load predefined inserts
@Rollback
public class VocabularyDaoTest {

    private static final String LOGO_PNG = "http://logo.png";

    @Autowired
    private VocabularyDao vocabularyDao;

    @Test
    public void testSelectAllByClassId() {
        OwnerId classOwner = new OwnerId("class001", "class");

        List<Vocabulary> results = vocabularyDao.selectAll(classOwner);
        assertEquals(2, results.size());

        Vocabulary firstVocab = results.stream()
                .filter(x -> x.getVocabId() == 101)
                .findFirst().orElseThrow();
        assertEquals("Vocabulary Basics", firstVocab.getName());
        assertEquals(4, firstVocab.getWordsTotal()); // Assume 4 words in 1st vocab
        assertNotNull(firstVocab.getCreateTime());
        assertNotNull(firstVocab.getUpdateTime());

        Vocabulary secondVocab = results.stream()
                .filter(x -> x.getVocabId() == 102)
                .findFirst().orElseThrow();
        assertEquals("Grammar 101", secondVocab.getName());
        assertEquals(7, secondVocab.getWordsTotal()); // Assume 6 words in 2nd vocab
        assertNotNull(firstVocab.getCreateTime());
        assertNotNull(secondVocab.getUpdateTime());
    }

    @Test
    public void testInsert() {
        OwnerId classOwner = new OwnerId("class003", "class");
        Vocabulary vocabulary = new Vocabulary("New Vocabulary", LOGO_PNG);

        // insert new vocabulary
        Vocabulary inserted = vocabularyDao.insert(classOwner, vocabulary);
        assertNotNull(inserted);
        assertEquals("New Vocabulary", inserted.getName());
        assertEquals(LOGO_PNG, inserted.getPictureUrl());
        assertFalse(inserted.isShared());
        assertEquals(0, inserted.getWordsTotal());
        assertNotNull(inserted.getCreateTime());
        assertNotNull(inserted.getUpdateTime());

        // verify presence in DB
        List<Vocabulary> results = vocabularyDao.selectAll(classOwner);
        assertEquals(1, results.size());
        assertEquals("New Vocabulary", results.getFirst().getName());
    }

    @Test
    public void testInsert_whenNameCollision() {
        OwnerId classOwner = new OwnerId("class001", "class");
        Vocabulary vocabulary = new Vocabulary("Vocabulary Basics", LOGO_PNG);

        // insert new vocabulary and expect name collision
        assertThrows(DuplicateVocabularyException.class, () -> vocabularyDao.insert(classOwner, vocabulary));
    }

    @Test
    public void testRename() {
        OwnerId classOwner = new OwnerId("class001", "class");
        // rename vocabulary and verify
        Vocabulary result = vocabularyDao.rename(classOwner, 101, "Updated Vocabulary Basics");
        assertNotNull(result);
        assertEquals("Updated Vocabulary Basics", result.getName());
        assertTrue(Instant.now().minusSeconds(3).isBefore(result.getUpdateTime()));
    }

    @Test
    public void testRename_whenNameCollision() {
        OwnerId classOwner = new OwnerId("class001", "class");
        // rename vocabulary and expect name collision
        assertThrows(DuplicateVocabularyException.class, () -> vocabularyDao.rename(classOwner, 102, "Vocabulary Basics"));
    }

    @Test
    public void testUpdatePictureUrl() throws DaoException {
        // update an existed vocabulary
        int updated = vocabularyDao.updatePicture(1, LOGO_PNG);
        assertEquals(1, updated);

        // verify after
        Vocabulary actual = vocabularyDao.selectById(1)
                .orElseThrow();
        assertNotNull(actual);
        assertEquals(1, actual.getVocabId());
        assertEquals("vocabulary1", actual.getName());
        assertEquals(LOGO_PNG, actual.getPictureUrl());
    }

    @Test
    public void testSetIsShared() {
        // update sharing status and verify
        Vocabulary result = vocabularyDao.updateIsShared(101, true);
        assertNotNull(result);
        assertTrue(result.isShared());
        assertEquals(4, result.getWordsTotal());
        assertTrue(Instant.now().minusSeconds(3).isBefore(result.getUpdateTime()));
    }

    @Test
    public void testSelectById() {
        // assume vocabulary id 101 exists in test-data.sql
        Optional<Vocabulary> vocabulary = vocabularyDao.selectById(101);

        assertTrue(vocabulary.isPresent());
        Vocabulary entity = vocabulary.get();
        assertEquals(101, entity.getVocabId());
        assertEquals("Vocabulary Basics", entity.getName());
        assertFalse(entity.isShared());
        assertEquals(4, entity.getWordsTotal()); // assume 10 words for id 101 in test-data.sql
    }

    @Test
    public void testSelectByIdNotFound() {
        Optional<Vocabulary> vocabulary = vocabularyDao.selectById(100500);

        assertTrue(vocabulary.isEmpty());
    }

    @Test
    void hasAccess() {
        OwnerId user = new OwnerId("john-123", "user");
        boolean hasAccess = vocabularyDao.hasAccess(user, 1);
        assertTrue(hasAccess);
    }

    @Test
    public void testDeleteVocabularyById() {
        int deleted = vocabularyDao.deleteVocabularyById(101);
        assertEquals(1, deleted);

        // Verify vocabulary is deleted
        Optional<Vocabulary> deletedVocabulary = vocabularyDao.selectById(101);
        assertTrue(deletedVocabulary.isEmpty());

        // Verify associated word references are deleted
        Set<Integer> wordsRefs = vocabularyDao.getWordIds(101);
        assertTrue(wordsRefs.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideIdsAdd")
    public void testAddRefsToVocabulary(int vocabId, Integer[] toAdd, int expectedTotal) {
        vocabularyDao.addWordsToVocabulary(vocabId, toAdd);

        Set<Integer> wordsRefs = vocabularyDao.getWordIds(vocabId);
        assertEquals(expectedTotal, wordsRefs.size());
        // verify added references
        assertTrue(wordsRefs.containsAll(Set.of(toAdd)));
        // verify updateTime
        Vocabulary result = vocabularyDao.selectById(vocabId)
                .orElseThrow(() -> new AssertionError("Vocabulary not found"));
        assertTrue(
                Instant.now().minusSeconds(3).isBefore(result.getUpdateTime()),
                "Expected updateTime to be updated recently"
        );
    }

    @Test
    public void testRemoveRefsFromVocabulary() {
        vocabularyDao.removeWordsFromVocabulary(101, 10, 13);

        Set<Integer> wordsRefs = vocabularyDao.getWordIds(101);
        assertEquals(Set.of(11, 12), wordsRefs);
        // verify updateTime
        Vocabulary result = vocabularyDao.selectById(101)
                .orElseThrow(() -> new AssertionError("Vocabulary not found"));
        assertTrue(
                Instant.now().minusSeconds(3).isBefore(result.getUpdateTime()),
                "Expected updateTime to be updated recently"
        );
    }

    @ParameterizedTest
    @MethodSource(value = "provideIdsGet")
    public void testGetRefsByVocabularyId(int vocabId, Set<Integer> expected) {
        Set<Integer> wordsRefs = vocabularyDao.getWordIds(vocabId);

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
                Arguments.of(101, new Integer[]{14, 15, 16, 17, 18, 19, 20}, 11),
                Arguments.of(102, new Integer[]{10, 11, 12, 13}, 11),
                Arguments.of(103, new Integer[]{10, 20}, 2)
        );
    }

}