package get.wordy.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(classes = {CardHeadlineDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CardHeadlineDaoTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Autowired
    private CardHeadlineDao cardHeadlineDao;

    @Test
    public void testGetCardsForDictionary() {
        // Define test data
        int dictionaryId = 1; // Example dictionary ID for testing

        // Execute DAO method
        List<Card> cards = cardHeadlineDao.getCardsForDictionary(dictionaryId);

        // Assert results
        assertEquals(1, cards.size());
        Card card = cards.getFirst();

        String formattedInstant = DATE_TIME_FORMATTER.format(card.getInsertedAt());

        List<String> sentences = card.getStrSentences();
        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(card.getId()).isEqualTo(1),
                () -> assertThat(card.getWordId()).isEqualTo(1),
                () -> assertThat(card.getScore()).isEqualTo(50),
                () -> assertThat(card.getStatus()).isEqualTo(CardStatus.DEFAULT_STATUS),
                () -> assertThat(formattedInstant).isEqualTo("2014-08-17 17:40:03"),
                () -> assertThat(card.getUpdatedAt()).isNotNull(),

                () -> assertEquals(3, sentences.size()),
                () -> assertEquals("sentence1", sentences.getFirst()),
                () -> assertEquals("sentence4", sentences.getLast()),

                () -> assertEquals(1, card.getCollocations().size()),
                () -> assertEquals("collocation1", card.getCollocations().getFirst())
        );
        Word word = card.getWord();
        assertNotNull(word);
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, word.getId()),
                () -> assertEquals("example1", word.getValue()),
                () -> assertEquals("ɪgˈzɑːmpl", word.getTranscription()),
                () -> assertEquals("noun", word.getPartOfSpeech()),
                () -> assertEquals("a word in dic 1", word.getMeaning())
        );
    }

    @Test
    public void testGetCard() {
        // Define test data
        int cardId = 1; // Example card ID for testing

        // Execute DAO method
        Card card = cardHeadlineDao.getCardById(cardId);

        // Assert results

        String formattedInstant = DATE_TIME_FORMATTER.format(card.getInsertedAt());

        List<String> sentences = card.getStrSentences();
        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(card.getId()).isEqualTo(1),
                () -> assertThat(card.getWordId()).isEqualTo(1),
                () -> assertThat(card.getScore()).isEqualTo(50),
                () -> assertThat(card.getStatus()).isEqualTo(CardStatus.DEFAULT_STATUS),
                () -> assertThat(formattedInstant).isEqualTo("2014-08-17 17:40:03"),
                () -> assertThat(card.getUpdatedAt()).isNotNull(),

                () -> assertEquals(3, sentences.size()),
                () -> assertEquals("sentence1", sentences.getFirst()),
                () -> assertEquals("sentence4", sentences.getLast()),

                () -> assertEquals(1, card.getCollocations().size()),
                () -> assertEquals("collocation1", card.getCollocations().getFirst())
        );
        Word word = card.getWord();
        assertNotNull(word);
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, word.getId()),
                () -> assertEquals("example1", word.getValue()),
                () -> assertEquals("ɪgˈzɑːmpl", word.getTranscription()),
                () -> assertEquals("noun", word.getPartOfSpeech()),
                () -> assertEquals("a word in dic 1", word.getMeaning())
        );
    }

    @Test
    void testGetCardsForExercise() {
        // Define test data
        int cardIds = 1; // Example card ID for testing

        // Execute DAO method
        List<Exercise> cards = cardHeadlineDao.getCardsForExercise(cardIds);

        // Assert results
        assertEquals(1, cards.size());
        Exercise card = cards.getFirst();

        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(card.getCardId()).isEqualTo(1),
                () -> assertThat(card.getWordId()).isEqualTo(1),

                () -> assertEquals(3, card.getSentences().size()),
                () -> assertEquals("sentence1", card.getSentences().getFirst().getExample()),
                () -> assertEquals("sentence3", card.getSentences().get(1).getExample()),
                () -> assertEquals("sentence4", card.getSentences().getLast().getExample())
        );
        Word word = card.getWord();
        assertNotNull(word);
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, word.getId()),
                () -> assertEquals("example1", word.getValue()),
                () -> assertEquals("ɪgˈzɑːmpl", word.getTranscription()),
                () -> assertEquals("noun", word.getPartOfSpeech()),
                () -> assertEquals("a word in dic 1", word.getMeaning())
        );
    }

}
