package get.wordy.core.api.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WordTest {

    @Test
    public void testEquals() {
        // Create two words with identical properties
        Word word1 = new Word(1, "hello", "noun", "/həˈləʊ/", "an expression of greeting");
        word1.addSentence(InContext.of("Sentence test 1").withMatchedWords("test"));
        word1.addStrSentence("Sentence 2");
        word1.addCollocation("Collocation 1");
        word1.addCollocation("Collocation 2");

        Word word2 = new Word(1, "hello", "noun", "/həˈləʊ/", "an expression of greeting");
        word2.addSentence(new InContext("Sentence test 1").withMatchedWords("test"));
        word2.addStrSentence("Sentence 2");
        word2.addCollocation("Collocation 1");
        word2.addCollocation("Collocation 2");

        // Assert that the two words are equal
        assertEquals(word1, word2);
    }

    @Test
    public void testNotEquals() {
        // Create two words with different properties
        Word word1 = new Word(1, "hello", "noun", "", "an expression of greeting");
        word1.addStrSentence("Sentence 1");
        word1.addStrSentence("Sentence 2");
        word1.addCollocation("Collocation 1");
        word1.addCollocation("Collocation 2");

        Word word2 = new Word(1, "hello", "noun", "", "An expression of greeting"); // Uppercased meaning
        word2.addSentence(new InContext("Sentence 1").withMatchedWords("test")); // Different
        word2.addStrSentence("Sentence 2");
        word2.addCollocation("Collocation 1");
        word2.addCollocation("Collocation 2");

        // Assert that the two words are not equal
        assertNotEquals(word1, word2);
    }

}
