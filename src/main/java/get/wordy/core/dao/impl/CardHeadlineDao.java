package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.dao.impl.helper.SentenceParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CardHeadlineDao {

    private static final String ALL_JOINS_QUERY = """
            SELECT
                cards.id AS card_id,
                cards.dictionary_id AS dictionary_id,
                cards.status,
                cards.score,
                cards.create_time,
                cards.last_update_time,
                words.id AS word_id,
                words.word,
                words.part_of_speech,
                words.transcription,
                words.meaning,
                array_remove(array_agg(DISTINCT context.example), NULL) AS card_sentences,
                array_remove(array_agg(DISTINCT collocations.example), NULL) AS card_collocations
            FROM
                cards
            JOIN
                words ON cards.word_id = words.id
            LEFT JOIN
                context ON cards.id = context.card_id
            LEFT JOIN
                collocations ON cards.id = collocations.card_id
            WHERE
                cards.dictionary_id = :dictionaryId
            GROUP BY
                cards.id, words.id
            """;

    private static final String GET_CARD_HEADLINE = """
            SELECT
                cards.id AS card_id,
                cards.dictionary_id AS dictionary_id,
                cards.status,
                cards.score,
                cards.create_time,
                cards.last_update_time,
                words.id AS word_id,
                words.word,
                words.part_of_speech,
                words.transcription,
                words.meaning,
                array_remove(array_agg(DISTINCT context.example), NULL) AS card_sentences,
                array_remove(array_agg(DISTINCT collocations.example), NULL) AS card_collocations
            FROM
                cards
            JOIN
                words ON cards.word_id = words.id
            LEFT JOIN
                context ON cards.id = context.card_id
            LEFT JOIN
                collocations ON cards.id = collocations.card_id
            WHERE
                cards.id = :cardId -- Specify the card ID to retrieve
            GROUP BY
                cards.id, words.id
            """;

    private static final String GET_CARDS_FOR_EXERCISE = """
            SELECT
                cards.id AS card_id,
                words.id AS word_id,
                words.word,
                words.part_of_speech,
                words.transcription,
                words.meaning,
                array_remove(
                    array_agg(DISTINCT 'example:' || context.example || ';' || 'matchedWords:' || context.matched_words),
                    NULL
                ) AS exercise_sentences
            FROM
                cards
                    JOIN
                words ON cards.word_id = words.id
                    LEFT JOIN
                context ON cards.id = context.card_id
            WHERE
                cards.id IN (:cardIds)
            GROUP BY
                cards.id, words.id;
            """;

    private static final String SELECT_FROM_CONTEXT_QUERY = "SELECT * FROM context WHERE matched_words IS NOT NULL AND card_id IN (:cardIds)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public CardHeadlineDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Card> getCardsForDictionary(int dictionaryId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("dictionaryId", dictionaryId);
        return jdbcTemplate.query(ALL_JOINS_QUERY, parameters, new FullCardRowMapper());
    }

    public Card getCardById(int cardId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("cardId", cardId);
        return jdbcTemplate.queryForObject(GET_CARD_HEADLINE, parameters, new FullCardRowMapper());
    }

    public List<Exercise> getCardsForExercise(int... cardIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        List<Integer> integers = Arrays.stream(cardIds)
                .boxed()
                .toList();
        parameters.addValue("cardIds", integers);
        return jdbcTemplate.query(GET_CARDS_FOR_EXERCISE, parameters, new ExerciseRowMapper());
    }

    public Map<Integer, List<Sentence>> getSentencesFor(int... cardIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        List<Integer> integers = Arrays.stream(cardIds)
                .boxed()
                .toList();
        parameters.addValue("cardIds", integers);
        return jdbcTemplate.query(SELECT_FROM_CONTEXT_QUERY, parameters, new SentencesMapper());
    }

    private static class FullCardRowMapper implements RowMapper<Card> {

        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            int cardId = rs.getInt("card_id");
            int dictionaryId = rs.getInt("dictionary_id");
            String status = rs.getString("status");
            int score = rs.getInt("score");
            int wordId = rs.getInt("word_id");

            Card cardData = new Card();
            cardData.setId(cardId);
            cardData.setDictionaryId(dictionaryId);
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

            String[] cardSentences = (String[]) rs.getArray("card_sentences").getArray();
            String[] cardCollocations = (String[]) rs.getArray("card_collocations").getArray();

            List<String> sentences = Arrays.asList(cardSentences);
            List<String> collocations = Arrays.asList(cardCollocations);

            cardData.setStrSentences(sentences);
            cardData.setCollocations(collocations);

            return cardData;
        }
    }

    private static class ExerciseRowMapper implements RowMapper<Exercise> {
        @Override
        public Exercise mapRow(ResultSet rs, int rowNum) throws SQLException {
            int cardId = rs.getInt("card_id");
            int wordId = rs.getInt("word_id");

            Exercise exercise = new Exercise();
            exercise.setCardId(cardId);
            exercise.setWordId(wordId);

            Word word = new Word(wordId,
                    rs.getString("word"),
                    rs.getString("part_of_speech"),
                    rs.getString("transcription"),
                    rs.getString("meaning"));
            exercise.setWord(word);

            Array exerciseSentencesArr = rs.getArray("exercise_sentences");
            exercise.setSentences(asModelList((String[]) exerciseSentencesArr.getArray()));

            return exercise;
        }

        private static List<Sentence> asModelList(String[] exerciseSentences) {
            final SentenceParser parser = new SentenceParser();
            return Arrays.stream(exerciseSentences)
                    .map(parser::parseSentence)
                    .toList();
        }
    }

    private static class SentencesMapper implements ResultSetExtractor<Map<Integer, List<Sentence>>> {

        @Override
        public Map<Integer, List<Sentence>> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<Sentence> result = new ArrayList<>();
            while (rs.next()) {
                int cardId = rs.getInt("card_id");
                String example = rs.getString("example");
                String matchedWords = rs.getString("matched_words");
                Sentence sentence = new Sentence(example, cardId)
                        .withMatchedWords(matchedWords);
                result.add(sentence);
            }
            return result
                    .stream()
                    .collect(Collectors.groupingBy(Sentence::getCardId));
        }
    }

}
