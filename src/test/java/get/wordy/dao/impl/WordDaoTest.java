package get.wordy.dao.impl;

import get.wordy.core.api.bean.Sentence;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.impl.WordDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {WordDao.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class WordDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_WORDS_CNT = 3;
    private static final int EXPECTED_NEW_ID = 4;

    @Autowired
    private WordDao wordDao;

    @Test
    public void insert() throws DaoException {
        Word word = new Word("apple", "noun", "transcription", "Some text", "A1");
        Sentence testSentence = Sentence.of("Test sentence")
                .withMatchedWords("test");
        word.addSentence(testSentence);
        word.addCollocation("Test collocation");

        Word inserted = wordDao.insert(word);
        assertTrue(inserted.getId() >= EXPECTED_NEW_ID);

        Set<Integer> ids = Set.of(1, 2, 3, inserted.getId());
        List<Word> words = wordDao.findAllByIds(ids);

        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        int id = 1;
        for (Word actual : words) {
            if (Objects.equals(actual.getId(), inserted.getId())) {
                assertEquals(word.getLemma(), actual.getLemma());
                assertEquals(word.getPartOfSpeech(), actual.getPartOfSpeech());
                assertEquals(word.getTranscription(), actual.getTranscription());
                assertEquals(word.getMeaning(), actual.getMeaning());
                assertSentences(word.getSentences(), actual.getSentences());
                assertCollocations(word.getCollocations(), actual.getCollocations());
            } else {
                assertEquals(id, actual.getId());
                assertEquals("example" + id, actual.getLemma());
                assertNotNull(actual.getTranscription());
            }
            id++;
        }
    }

    @Test
    public void update() throws DaoException {
        for (int id = 1; id <= PREDEFINED_WORDS_CNT; id++) {
            Word word = new Word(id, "to test " + id, "VERB", "transcription" + id, "test", "A1");
            Sentence testSentence = Sentence.of("Test sentence").withMatchedWords("test");
            word.addSentence(testSentence);
            word.addStrSentence("Test sentence 2");
            word.addCollocation("Test collocation");

            int updated = wordDao.update(word);
            assertEquals(1, updated);

            Word updatedWord = wordDao.findById(id);
            assertNotNull(updatedWord);
            assertEquals(word.getLemma(), updatedWord.getLemma());
            assertEquals(word.getPartOfSpeech(), updatedWord.getPartOfSpeech());
            assertEquals(word.getTranscription(), updatedWord.getTranscription());
            assertEquals(word.getMeaning(), updatedWord.getMeaning());
            assertSentences(word.getSentences(), updatedWord.getSentences());
            assertCollocations(word.getCollocations(), updatedWord.getCollocations());
        }
    }

    @Test
    public void delete_whenAbandonedWord() throws DaoException {
        int abandonedWordId = 3;
        wordDao.delete(abandonedWordId);

        List<Word> wordsAfter = wordDao.findAllByIds(Set.of(1, 2, 3));
        assertNotNull(wordsAfter);
        assertEquals(PREDEFINED_WORDS_CNT - 1, wordsAfter.size());
        assertTestData(wordsAfter);
    }

    @Test
    public void delete_whenViolationException() throws DaoException {
        int wordIdReferenced = 1;
        DaoException daoException = assertThrows(DaoException.class,
                () -> wordDao.delete(wordIdReferenced)
        );
        assertEquals("Error while deleting a word record", daoException.getMessage());
    }

    @Test
    public void findAllByIds() throws DaoException {
        List<Word> words = wordDao.findAllByIds(Set.of(1, 2, 3, 4));
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());
        assertTestData(words);
    }

    @Test
    public void addWords() {
        List<Word> words = generateTestData();
        List<Word> inserted = wordDao.addWords(words);

        List<Integer> generatedIds = inserted.stream().map(Word::getId).toList();
        Set<Integer> allIds = new HashSet<>(Set.of(1, 2, 3));
        allIds.addAll(generatedIds);

        List<Word> allWordsAfter = wordDao.findAllByIds(allIds);
        assertNotNull(allWordsAfter);
        assertEquals(PREDEFINED_WORDS_CNT + words.size(), allWordsAfter.size());

        Map<String, Word> returnedByValue = inserted.stream()
                .collect(Collectors.toMap(Word::getLemma, w -> w));

        for (Word expected : words) {
            Word actual = returnedByValue.get(expected.getLemma());
            assertNotNull(actual, "Missing word: " + expected.getLemma());
            assertTrue(actual.getId() >= EXPECTED_NEW_ID);
            assertEquals(expected.getLemma(), actual.getLemma());
            assertSentences(expected.getSentences(), actual.getSentences());
            assertCollocations(expected.getCollocations(), actual.getCollocations());
        }
    }

    private static void assertTestData(List<Word> words) {
        for (int i = 0, id = 1; i < words.size(); i++, id++) {
            Word next = words.get(i);
            assertEquals(id, next.getId());
            assertEquals("example" + id, next.getLemma());
            assertNotNull(next.getTranscription());
            assertEquals("noun", next.getPartOfSpeech().toLowerCase());
            assertNotNull(next.getMeaning());
        }
    }

    @Test
    void findById() throws DaoException {
        var word = wordDao.findById(1);
        assertNotNull(word);
        assertEquals(1, word.getId());
        assertEquals("example1", word.getLemma());
        assertTrue(word.getMeaning().contains("a word"));
        assertEquals("noun", word.getPartOfSpeech());
        assertNotNull(word.getTranscription());
    }

    @Test
    void findById_whenNotExists() throws DaoException {
        var word = wordDao.findById(100);
        assertNull(word);
    }

    @Test
    void findByLemma_withTypo() throws DaoException {
        List<Word> words = wordDao.findByLemma("exampl1");
        assertFalse(words.isEmpty());
        var word = words.getFirst();
        assertNotNull(word);
        assertEquals(1, word.getId());
        assertEquals("example1", word.getLemma());
        assertTrue(word.getMeaning().contains("a word"));
        assertEquals("noun", word.getPartOfSpeech());
        assertNotNull(word.getTranscription());
    }

    @Sql(value = "/themes.sql", config = @SqlConfig(encoding = "utf-8"))
    @Test
    void findExistingWords() {
        Set<String> input = Set.of(
                "airport",
                "PASSPORT",
                "unknownWord"
        );

        List<Word> result = wordDao.findExistingWords(input);
        assertEquals(2, result.size());

        assertEquals("Airport", result.getFirst().getLemma());
        assertEquals("noun", result.getFirst().getPartOfSpeech());
        assertEquals("Passport", result.get(1).getLemma());
        assertEquals("noun", result.get(1).getPartOfSpeech());
    }

    private static void assertSentences(List<Sentence> expected, List<Sentence> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).example(), actual.get(i).example());
        }
    }

    private static void assertCollocations(List<String> expected, List<String> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    public List<Word> generateTestData() {
        // list of words to base the test data on
        List<String> strings = List.of("Explain", "Plan", "Singleton", "Generated", "Impediments");
        // generating realistic test data for words
        return strings.stream()
                .map(value -> {
                    Word word = new Word(value, "noun", "some transcription", "a sample meaning", "A1");
                    word.setStrSentences(generateSentences(value));
                    word.setCollocations(generateCollocations(value));
                    return word;
                })
                .toList();
    }

    private List<String> generateSentences(String lemma) {
        return List.of(
                "The " + lemma + " is a common term used in the industry.",
                "Many people find the " + lemma + " concept difficult to understand.",
                "It is crucial to grasp the idea of " + lemma + " for better performance.",
                lemma + " is often misunderstood in discussions about technology."
        );
    }

    private List<String> generateCollocations(String lemma) {
        return switch (lemma.toLowerCase()) {
            case "explain" -> List.of("explain in detail", "explain clearly", "explain further");
            case "plan" -> List.of("long-term plan", "strategic plan", "action plan");
            case "singleton" -> List.of("singleton pattern", "singleton class", "singleton instance");
            case "generated" -> List.of("generated data", "generated content", "automatically generated");
            case "impediments" -> List.of("overcome impediments", "remove impediments", "impediments to success");
            default -> List.of("common collocation");
        };
    }

}