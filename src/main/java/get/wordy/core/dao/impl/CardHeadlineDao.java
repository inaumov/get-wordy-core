package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@Repository
public class CardHeadlineDao {

    private static final String ALL_JOINS_QUERY = """
            SELECT
                cards.id AS card_id,
                cards.status,
                cards.score,
                cards.create_time,
                cards.last_update_time,
                words.id AS word_id,
                words.word,
                words.part_of_speech,
                words.transcription,
                words.meaning,
                array_remove(array_agg(DISTINCT context.example), NULL) AS context_examples,
                array_remove(array_agg(DISTINCT collocations.example), NULL) AS collocation_examples
            FROM
                cards
            JOIN
                words ON cards.word_id = words.id
            LEFT JOIN
                context ON cards.id = context.card_id
            LEFT JOIN
                collocations ON cards.id = collocations.card_id
            WHERE
                cards.dictionary_id = ?
            GROUP BY
                cards.id, words.id
            """;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CardHeadlineDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Card> getCardsForDictionary(int dictionaryId) {
        return jdbcTemplate.query(ALL_JOINS_QUERY, new CardDataRowMapper(), dictionaryId);
    }

    public static class CardDataRowMapper implements RowMapper<Card> {

        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            int cardId = rs.getInt("card_id");
            String status = rs.getString("status");
            int score = rs.getInt("score");
            int wordId = rs.getInt("word_id");

            Card cardData = new Card();
            cardData.setId(cardId);
            cardData.setStatus(CardStatus.valueOf(status));
            cardData.setScore(score);
            cardData.setWordId(wordId);
            Timestamp createTime = rs.getTimestamp("create_time");
            if (createTime != null) {
                cardData.setInsertedAt(createTime.toInstant());
            }
            Timestamp updateTime = rs.getTimestamp("last_update_time");
            if (updateTime != null) {
                cardData.setUpdatedAt(updateTime.toInstant());
            }
            Word word = new Word(wordId,
                    rs.getString("word"),
                    rs.getString("part_of_speech"),
                    rs.getString("transcription"),
                    rs.getString("meaning"));
            cardData.setWord(word);

            String[] contextExamples = (String[]) rs.getArray("context_examples").getArray();
            String[] collocationExamples = (String[]) rs.getArray("collocation_examples").getArray();

            List<Context> contexts = toContexts(cardId, contextExamples);
            List<Collocation> collocations = toCollocations(cardId, collocationExamples);

            cardData.setContexts(contexts);
            cardData.setCollocations(collocations);

            return cardData;
        }

        private List<Context> toContexts(int cardId, String[] contextExamples) {
            return Arrays.stream(contextExamples)
                    .map(sentence -> new Context(0, sentence, cardId))
                    .toList();
        }

        private List<Collocation> toCollocations(int cardId, String[] collocationExamples) {
            return Arrays.stream(collocationExamples)
                    .map(collocation -> new Collocation(0, collocation, cardId))
                    .toList();
        }

    }

}
