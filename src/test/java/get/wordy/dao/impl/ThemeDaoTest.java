package get.wordy.dao.impl;

import get.wordy.core.api.bean.Theme;
import get.wordy.core.api.bean.ThemeStatus;
import get.wordy.core.api.exception.DuplicateThemeException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.impl.ThemeDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Sql(scripts = "/themes.sql")
@SpringJUnitConfig(classes = {ThemeDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ThemeDaoTest extends BaseDaoTest {

    @Autowired
    private ThemeDao themeDao;

    private final OwnerId ownerId =
            new OwnerId("john-123", "user");

    private final OwnerId anotherOwner =
            new OwnerId("mike-456", "user");

    @Test
    void testFindAll() {
        var themes = themeDao.findAll(ownerId);

        assertEquals(2, themes.size());
        assertEquals("Animals", themes.getFirst().name());
        assertEquals(2, themes.getFirst().wordsTotal());

        themes = themeDao.findAll(anotherOwner);
        assertEquals(2, themes.size());
        assertEquals("Pets", themes.getFirst().name());
        assertEquals(5, themes.getFirst().wordsTotal());
        assertEquals("Sports", themes.getLast().name());
        assertEquals(0, themes.getLast().wordsTotal());
    }

    @Test
    void testFindAllReturnsEmpty() {
        assertTrue(themeDao.findAll(
                new OwnerId("unknown", "user")
        ).isEmpty());
    }

    @Test
    void testFindById() {
        var theme = themeDao.findById(ownerId, 1).orElseThrow();

        assertEquals(1, theme.themeId());
        assertEquals("Animals", theme.name());
        assertEquals(2, theme.wordsTotal());

        theme = themeDao.findById(anotherOwner, 4).orElseThrow();
        assertEquals("Pets", theme.name());
        assertEquals(5, theme.wordsTotal());
    }

    @Test
    void testFindByIdReturnsEmpty() {
        assertTrue(themeDao.findById(ownerId, 999).isEmpty());
    }

    @Test
    void hasAccess() {
        OwnerId user = new OwnerId("john-123", "user");
        boolean hasAccess = themeDao.hasAccess(user, 1);
        assertTrue(hasAccess);
    }

    @Test
    void testCreate() {
        var created = themeDao.create(ownerId, "Travel");
        var actual = themeDao.findById(ownerId, created.themeId()).orElseThrow();

        assertEquals("Travel", actual.name());
        assertEquals(0, actual.wordsTotal());
    }

    @Test
    void testCreateDuplicateThrowsException() {
        assertThrows(
                DuplicateThemeException.class,
                () -> themeDao.create(ownerId, "Animals")
        );
    }

    @Test
    void testRename() {
        Theme renamed = themeDao.rename(ownerId, 1, "animals");
        assertEquals("animals", renamed.name());

        renamed = themeDao.rename(ownerId, 1, "ANIMALS");
        assertEquals("ANIMALS", renamed.name());

        themeDao.rename(ownerId, 1, "Wild Animals");

        Optional<Theme> result = themeDao.findById(ownerId, 1);
        assertEquals(
                "Wild Animals",
                result.orElseThrow().name()
        );
        assertTrue(Instant.now().minusSeconds(3).isBefore(result.get().lastUpdatedAt()));
    }

    @Test
    void testRenameDuplicateThrowsException() {
        assertThrows(
                DuplicateThemeException.class,
                () -> themeDao.rename(ownerId, 1, "Food")
        );
    }

    @Test
    void testUpdateStatus() {
        Theme updated = themeDao.updateStatus(ownerId, 1, ThemeStatus.GENERATING);
        assertEquals(
                ThemeStatus.GENERATING,
                updated.status()
        );

        assertEquals(
                ThemeStatus.GENERATING,
                themeDao.findById(ownerId, 1).orElseThrow().status()
        );
    }

    @Test
    void testDelete() {
        assertTrue(themeDao.delete(ownerId, 1));
        assertTrue(themeDao.findById(ownerId, 1).isEmpty());
    }

    @Test
    void testDeleteReturnsFalse() {
        assertFalse(themeDao.delete(ownerId, 999));
    }

    @Test
    void testAddWordsToTheme() {
        themeDao.addWordsToTheme(1, 1, 2);

        assertEquals(
                Set.of(1, 2),
                themeDao.getWordIds(1)
        );
    }

    @Test
    void testAddWordsToThemeIgnoresDuplicates() {
        themeDao.addWordsToTheme(1, 1, 2);

        assertEquals(
                Set.of(1, 2),
                themeDao.getWordIds(1)
        );
    }

    @Test
    void testRemoveWordsFromTheme() {
        themeDao.removeWordsFromTheme(1, 1);

        assertEquals(
                Set.of(2),
                themeDao.getWordIds(1)
        );
    }

    @Test
    void testGetWordIds() {
        assertEquals(
                Set.of(1, 2),
                themeDao.getWordIds(1)
        );
    }

    @Test
    void testOwnerIsolation() {
        var another = new OwnerId("mike-123", "user");

        assertTrue(themeDao.findById(another, 1).isEmpty());
        assertFalse(themeDao.delete(another, 1));
    }

    @Test
    void testSaveCandidateWordsJson() {
        var draft = """
                [{
                "lemma":"run",
                "partOfSpeech":"verb",
                "meaning":"to move quickly on foot",
                "level":"A1"
                }]
                """;
        themeDao.saveCandidateWordsJson(anotherOwner, 3, draft);
        themeDao.updateStatus(anotherOwner, 3, ThemeStatus.DRAFT);
        var theme = themeDao.findById(anotherOwner, 3).orElseThrow();
        assertEquals(1, theme.wordsTotal());
    }

    @Test
    void testGetCandidateWordsJson() {
        var draft = themeDao.getCandidateWordsJson(anotherOwner, 4);
        assertNotNull(draft);
        assertTrue(draft.contains("lemma"));
    }

    @Test
    void testRemoveCandidateWord() {
        themeDao.saveCandidateWordsJson(
                anotherOwner,
                3,
                """
                        [
                          {
                            "lemma":"dog",
                            "partOfSpeech":"noun",
                            "meaning":"pet dog",
                            "level":"A1"
                          },
                          {
                            "lemma":"cat",
                            "partOfSpeech":"noun",
                            "meaning":"pet cat",
                            "level":"A1"
                          },
                          {
                            "lemma":"run",
                            "partOfSpeech":"verb",
                            "meaning":"move quickly",
                            "level":"A1"
                          }
                        ]
                        """
        );

        themeDao.removeCandidateWord(anotherOwner, 3, "dog", "noun");

        String json = themeDao.getCandidateWordsJson(anotherOwner, 3);
        assertNotNull(json);
        // removed:
        assertFalse(json.contains("dog"));
        // still present:
        assertTrue(json.contains("cat"));
        assertTrue(json.contains("run"));
    }

}
