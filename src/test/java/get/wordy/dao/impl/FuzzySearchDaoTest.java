package get.wordy.dao.impl;

import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.WordDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

@Sql(value = "/fuzzy-search.sql", config = @SqlConfig(encoding = "utf-8"))
@SpringJUnitConfig(classes = {WordDao.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FuzzySearchDaoTest extends BaseDaoTest {

    @Autowired
    private WordDao wordDao;

    @Test
    void findByLemma_withExactShortWord_returnsExactMatch() throws DaoException {
        List<Word> words = wordDao.findByLemma("cat");
        assertFalse(words.isEmpty());
        assertEquals("cat", words.getFirst().getLemma());
    }

    @Test
    void findByLemma_withDifferentShortWord_doesNotReturnCat() throws DaoException {
        List<Word> words = wordDao.findByLemma("bat");
        assertTrue(
                words.stream()
                        .noneMatch(word -> "cat".equals(word.getLemma()))
        );
    }

    @ParameterizedTest(name = "{0} should find {1} as the closest match")
    @CsvSource({
            "exampl,       example",
            "examples,     example",
            "accomodation, accommodation",
            "restarant,    restaurant",
            "enviroment,   environment",
            "beautifull,   beautiful",
            "neccessary,   necessary",
            "seperate,     separate",
            "catt,         cat"
    })
    void findByLemma_withCommonTypo_returnsClosestWordFirst(String typo, String expectedLemma) throws DaoException {
        List<Word> words = wordDao.findByLemma(typo);
        assertFalse(words.isEmpty());
        assertEquals(expectedLemma, words.getFirst().getLemma(), "Expected '%s' for typo '%s', but got: %s"
                .formatted(expectedLemma, typo, words));
    }

    @Test
    void findByLemma_withCompletelyUnrelatedWord_returnsEmptyList() throws DaoException {
        List<Word> words = wordDao.findByLemma("xyzqwerty");
        assertTrue(words.isEmpty());
    }

    @Test
    void findByLemma_withExactMatch_returnsWord() throws DaoException {
        List<Word> words = wordDao.findByLemma("accommodation");
        assertFalse(words.isEmpty());
        assertEquals("accommodation", words.getFirst().getLemma());
    }

    @Test
    void findByLemma_isCaseInsensitive() throws DaoException {
        List<Word> words = wordDao.findByLemma("ACCOMMODATION");
        assertTrue(
                words.stream()
                        .anyMatch(word ->
                                "accommodation".equals(word.getLemma()))
        );
    }

    @Test
    void findByLemma_withTypo_returnsCompleteWordData() throws DaoException {
        List<Word> words = wordDao.findByLemma("restarant");
        Word word = words.stream()
                .filter(candidate ->
                        "restaurant".equals(candidate.getLemma()))
                .findFirst()
                .orElseThrow();

        assertNotNull(word.getId());
        assertNotNull(word.getMeaning());
        assertNotNull(word.getPartOfSpeech());
        assertNotNull(word.getTranscription());
    }

}
