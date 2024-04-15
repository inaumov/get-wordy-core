package get.wordy.core.api.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WordTest {

    @Test
    public void testEquals() {
        // Create two words with identical properties
        Word word1 = new Word(1, "hello", "noun", "/həˈləʊ/", "an expression of greeting");
        Word word2 = new Word(1, "hello", "noun", "/həˈləʊ/", "an expression of greeting");

        // Assert that the two words are equal
        assertEquals(word1, word2);
    }

    @Test
    public void testNotEquals() {
        // Create two words with different properties
        Word word1 = new Word(1, "hello", "noun", "", "an expression of greeting");
        Word word2 = new Word(1, "hello", "noun", "", "An expression of greeting"); // Uppercased meaning

        // Assert that the two words are not equal
        assertNotEquals(word1, word2);
    }

}
