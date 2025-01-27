package get.wordy.dao.impl;

import get.wordy.core.api.bean.InContext;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.impl.WordDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class WordDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_WORDS_CNT = 3;
    private static final int EXPECTED_NEW_ID = 4;

    private WordDao wordDao;

    private static final Random random = new Random();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        wordDao = daoFactory.getWordDao();
        assertNotNull(wordDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Word word = new Word("apple", "noun", "transcription", "Some text");
        InContext testSentence = InContext.of("Test sentence")
                .withMatchedWords("test");
        word.addSentence(testSentence);
        word.addCollocation("Test collocation");

        Word inserted = wordDao.insert(word);
        assertTrue(inserted.getId() >= EXPECTED_NEW_ID);

        Set<Integer> ids = Set.of(1, 2, 3, inserted.getId());

        List<Word> words = wordDao.selectAll(ids);
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT + 1, words.size());

        int id = 1;
        for (Word actual : words) {
            if (actual.getId() >= EXPECTED_NEW_ID) {
                assertEquals(word.getValue(), actual.getValue());
                assertEquals("noun", actual.getPartOfSpeech());
                assertEquals("transcription", actual.getTranscription());
                assertEquals("Some text", actual.getMeaning());
                assertSentences(actual.getSentences(), wordDao.getSentencesFor(actual.getId()));
                assertCollocations(actual.getCollocations(), wordDao.getCollocationsFor(actual.getId()));
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
            Word word = new Word(id, "to test " + id, "VERB", "transcription" + id, "test");
            InContext testSentence = InContext.of("Test sentence")
                    .withMatchedWords("test");
            word.addSentence(testSentence);
            word.addStrSentence("Test sentence 2");
            word.addCollocation("Test collocation");
            int i = wordDao.update(word);
            assertEquals(1, i);
        }
        // count words after updating
        List<Word> words = wordDao.selectAll(Set.of(1, 2, 3));
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());

        int id = 1;
        for (Word actual : words) {
            assertEquals(id, actual.getId());
            assertEquals("to test " + id, actual.getValue());
            assertEquals("transcription" + id, actual.getTranscription());
            assertSentences(actual.getSentences(), wordDao.getSentencesFor(actual.getId()));
            assertCollocations(actual.getCollocations(), wordDao.getCollocationsFor(actual.getId()));
            id++;
        }
    }

    @Test
    public void testDeleteAbandonedWord() throws DaoException {
        int abandonedWordId = 3;
        wordDao.delete(abandonedWordId);
        List<Word> wordsAfter = wordDao.selectAll(Set.of(1, 2, 3));
        assertNotNull(wordsAfter);
        assertEquals(PREDEFINED_WORDS_CNT - 1, wordsAfter.size());
        assertTestData(wordsAfter);
    }

    @Test
    public void testDeleteWordViolationException() throws DaoException {
        int wordIdReferenced = 1;
        DaoException daoException = assertThrows(DaoException.class,
                () -> wordDao.delete(wordIdReferenced)
        );
        assertEquals("Error while deleting a word record", daoException.getMessage());
    }

    @Test
    public void testSelectAll() throws DaoException {
        List<Word> words = wordDao.selectAll(Set.of(1, 2, 3, 4));
        assertNotNull(words);
        assertEquals(PREDEFINED_WORDS_CNT, words.size());
        assertTestData(words);
    }

    @Test
    public void testAddWords() throws Exception {
        // generate realistic test data
        List<Word> words = generateTestData();
        List<Word> copyReturned = wordDao.addWords(words);

        List<Integer> generated = copyReturned
                .stream()
                .map(Word::getId)
                .toList();
        Set<Integer> all = new HashSet<>();
        all.add(1);
        all.add(2);
        all.add(3);
        all.addAll(generated);

        List<Word> allWordsAfter = wordDao.selectAll(all);
        assertNotNull(allWordsAfter);
        assertEquals(PREDEFINED_WORDS_CNT + words.size(), allWordsAfter.size());

        Map<String, Word> returnedWordsByValue = copyReturned.stream()
                .collect(Collectors.toMap(Word::getValue, word -> word));

        // now, check that each generated word is present in copyReturned
        for (Word expectedWord : words) {
            // look for the expected word in the returned map (by value or id)
            assertTrue(returnedWordsByValue.containsKey(expectedWord.getValue()),
                    "Missing word: " + expectedWord.getValue());

            // optional: You could also assert that the ID matches if you want to be more specific:
            Word actualWord = returnedWordsByValue.get(expectedWord.getValue());
            assertTrue(actualWord.getId() >= EXPECTED_NEW_ID, "Word id should not be 0.");;
            assertEquals(expectedWord.getValue(), actualWord.getValue(), "Word values should match.");
        }
    }

    private static void assertTestData(List<Word> words) {
        for (int i = 0, id = 1; i < words.size(); i++, id++) {
            Word next = words.get(i);
            assertEquals(id, next.getId());
            assertEquals("example" + id, next.getValue());
            assertNotNull(next.getTranscription());
            assertEquals("noun", next.getPartOfSpeech().toLowerCase());
            assertNotNull(next.getMeaning());
        }
    }

    @Test
    void testGetWord() throws DaoException {
        var word = wordDao.selectById(1);
        assertNotNull(word);
        assertEquals(1, word.getId());
        assertEquals("example1", word.getValue());
        assertTrue(word.getMeaning().contains("a word"));
        assertEquals("noun", word.getPartOfSpeech());
        assertNotNull(word.getTranscription());
    }

    @Test
    void testGetWordNotExists() throws DaoException {
        var word = wordDao.selectById(100);
        assertNull(word);
    }

    private static void assertSentences(List<InContext> expectedSentences, List<InContext> actualSentences) {
        assertNotNull(actualSentences);

        for (int i = 0; i < expectedSentences.size(); i++) {
            String expected = expectedSentences.get(i).getExample();
            InContext actual = actualSentences.get(i);
            assertEquals(expected, actual.getExample());
        }
    }

    private static void assertCollocations(List<String> expectedCollocations, List<String> actualCollocations) {
        assertNotNull(actualCollocations);

        for (int i = 0; i < expectedCollocations.size(); i++) {
            String expected = expectedCollocations.get(i);
            String actual = actualCollocations.get(i);
            assertEquals(expected, actual);
        }
    }

    public List<Word> generateTestData() {
        // list of words to base the test data on
        List<String> strings = List.of("Explain", "Plan", "Singleton", "Generated", "Impediments");
        // generating realistic test data for words
        return strings.stream()
                .map(value -> {
                    Word word = new Word(value, "noun", "some transcription", "a sample meaning");
                    word.setStrSentences(generateSentences(value));
                    word.setCollocations(generateCollocations(value));
                    return word;
                })
                .toList();
    }

    private List<String> generateSentences(String wordValue) {
        // generate some sentences that include the word (to make it more realistic)
        return List.of(
                "The " + wordValue + " is a common term used in the industry.",
                "Many people find the " + wordValue + " concept difficult to understand.",
                "It is crucial to grasp the idea of " + wordValue + " for better performance.",
                wordValue + " is often misunderstood in discussions about technology."
        );
    }

    private List<String> generateCollocations(String wordValue) {
        // simple predefined collocations that can be related to the word
        return switch (wordValue.toLowerCase()) {
            case "explain" -> List.of("explain in detail", "explain clearly", "explain further");
            case "plan" -> List.of("long-term plan", "strategic plan", "action plan");
            case "singleton" -> List.of("singleton pattern", "singleton class", "singleton instance");
            case "generated" -> List.of("generated data", "generated content", "automatically generated");
            case "impediments" -> List.of("overcome impediments", "remove impediments", "impediments to success");
            default -> List.of("common collocation");
        };
    }

}