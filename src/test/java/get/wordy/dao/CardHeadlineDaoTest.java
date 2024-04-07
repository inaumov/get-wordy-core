package get.wordy.dao;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(classes = {CardHeadlineDao.class, CardHeadlineDao.CardDataRowMapper.class, SpringJdbcConfig.class})
@JdbcTest
@Import(CardHeadlineDao.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CardHeadlineDaoTest {

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        String formattedInstant = formatter.format(card.getInsertedAt());

        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(card.getId()).isEqualTo(1),
                () -> assertThat(card.getWordId()).isEqualTo(1),
                () -> assertThat(card.getScore()).isEqualTo(50),
                () -> assertThat(card.getStatus()).isEqualTo(CardStatus.DEFAULT_STATUS),
                () -> assertThat(formattedInstant).isEqualTo("2014-08-17 17:40:03"),
                () -> assertThat(card.getUpdatedAt()).isNotNull(),

                () -> assertEquals(3, card.getSentences().size()),
                () -> assertEquals("sentence1", card.getSentences().getFirst()),
                () -> assertEquals("sentence4", card.getSentences().getLast()),

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

}
