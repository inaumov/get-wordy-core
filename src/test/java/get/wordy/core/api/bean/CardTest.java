package get.wordy.core.api.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardTest {

    @Test
    public void testEquals() {
        // Create two cards with identical properties
        Card card1 = new Card();
        card1.setId(1);
        card1.setDictionaryId(100);
        card1.setWordId(200);
        card1.setStatus(CardStatus.TO_LEARN);
        card1.setScore(25);

        Card card2 = new Card();
        card2.setId(1);
        card2.setDictionaryId(100);
        card2.setWordId(200);
        card2.setStatus(CardStatus.TO_LEARN);
        card2.setScore(25);

        // Assert that the two cards are equal
        assertEquals(card1, card2);
    }

    @Test
    public void testNotEquals() {
        // Create two cards with different properties
        Card card1 = new Card();
        card1.setId(1);
        card1.setDictionaryId(100);
        card1.setWordId(200);
        card1.setStatus(CardStatus.TO_LEARN);
        card1.setScore(25);

        Card card2 = new Card();
        card2.setId(1);
        card2.setDictionaryId(100);
        card2.setWordId(200);
        card2.setStatus(CardStatus.TO_LEARN);
        card2.setScore(50);

        // Assert that the two cards are not equal
        assertNotEquals(card1, card2);
    }

}
