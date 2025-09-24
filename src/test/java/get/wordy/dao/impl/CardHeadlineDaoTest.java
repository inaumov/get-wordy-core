package get.wordy.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(classes = {CardHeadlineDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CardHeadlineDaoTest {

    @Autowired
    private CardHeadlineDao cardHeadlineDao;

    @Test
    public void testGetWordsHeadlines() {

        int vocabId = 1;

        // Execute DAO method
        List<Word> words = cardHeadlineDao.getWordsHeadlines(vocabId);

        // Assert results
        assertEquals(1, words.size());
        Word word = words.getFirst();
        assertNotNull(word);

        List<String> sentences = word.getStrSentences();
        assertAll(
                "Grouped assertions of Word Headline",
                () -> assertThat(word.getId()).isEqualTo(1),

                () -> assertEquals(3, sentences.size()),
                () -> assertEquals("Test sentence 1", sentences.getFirst()),
                () -> assertEquals("Test sentence 4", sentences.getLast()),

                () -> assertEquals(1, word.getCollocations().size()),
                () -> assertEquals("collocation1", word.getCollocations().getFirst())
        );
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, word.getId()),
                () -> assertEquals("example1", word.getLemma()),
                () -> assertEquals("ɪgˈzɑːmpl", word.getTranscription()),
                () -> assertEquals("noun", word.getPartOfSpeech()),
                () -> assertEquals("a word in vocab 1", word.getMeaning())
        );
    }

    @Test
    public void testGetCard() {

        int wordId = 1;

        // Execute DAO method
        Word word = cardHeadlineDao.getWordHeadlineById(1, wordId);
        assertNotNull(word);

        // Assert results

        List<String> sentences = word.getStrSentences();
        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(word.getId()).isEqualTo(1),

                () -> assertEquals(3, sentences.size()),
                () -> assertEquals("Test sentence 1", sentences.getFirst()),
                () -> assertEquals("Test sentence 4", sentences.getLast()),

                () -> assertEquals(1, word.getCollocations().size()),
                () -> assertEquals("collocation1", word.getCollocations().getFirst())
        );
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, word.getId()),
                () -> assertEquals("example1", word.getLemma()),
                () -> assertEquals("ɪgˈzɑːmpl", word.getTranscription()),
                () -> assertEquals("noun", word.getPartOfSpeech()),
                () -> assertEquals("a word in vocab 1", word.getMeaning())
        );
    }

    @Test
    void testGetWordsHeadlinesForExercise() {
        // Execute DAO method
        List<FlashCard> cards = cardHeadlineDao.getFlashCards("john-123", 1);

        // Assert results
        assertEquals(1, cards.size());
        FlashCard card = cards.getFirst();

        assertAll(
                "Grouped assertions of Card Headline",
                () -> assertThat(card.wordId()).isEqualTo(1),

                // select only those which has both example and matched words value. as most viable
                () -> assertEquals(2, card.getStrSentences().size()),
                () -> assertEquals("Test sentence 1", card.sentences().getFirst().example()),
                () -> assertEquals("sentence 1", card.sentences().getFirst().matchedWords()),
                () -> assertEquals("Test sentence 4", card.sentences().getLast().example()),
                () -> assertEquals("sentence 4", card.sentences().getLast().matchedWords())
        );
        assertAll(
                "Grouped assertions of Word sub-entity",
                () -> assertEquals(1, card.wordId()),
                () -> assertEquals("example1", card.lemma()),
                () -> assertEquals("ɪgˈzɑːmpl", card.transcription()),
                () -> assertEquals("noun", card.partOfSpeech()),
                () -> assertEquals("a word in vocab 1", card.meaning())
        );
    }

}
