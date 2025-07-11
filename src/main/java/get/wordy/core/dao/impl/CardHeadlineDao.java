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
import org.springframework.util.StringUtils;

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
            SELECT c.id                                                         AS card_id,
                   c.vocab_id,
                   c.status,
                   c.score,
                   c.create_time,
                   c.last_update_time,
                   w.id                                                         AS word_id,
                   w.word,
                   w.part_of_speech,
                   w.transcription,
                   w.meaning,
                   array_remove(array_agg(DISTINCT in_context.example), NULL)   AS card_sentences,
                   array_remove(array_agg(DISTINCT collocations.example), NULL) AS card_collocations
            FROM vocab_has_words vhw
                JOIN words w ON vhw.word_ref = w.id
                LEFT JOIN in_context ON w.id = in_context.word_id
                LEFT JOIN collocations ON w.id = collocations.word_id
                LEFT JOIN cards c ON c.word_id = w.id AND c.vocab_id = vhw.vocab_id AND c.user_id = :userId
            WHERE vhw.vocab_id = :vocabId
            GROUP BY c.id, w.id;
            """;

    private static final String GET_CARD_HEADLINE = """
            SELECT
                cards.id AS card_id,
                cards.vocab_id,
                cards.status,
                cards.score,
                cards.create_time,
                cards.last_update_time,
                words.id AS word_id,
                words.word,
                words.part_of_speech,
                words.transcription,
                words.meaning,
                array_remove(array_agg(DISTINCT in_context.example), NULL) AS card_sentences,
                array_remove(array_agg(DISTINCT collocations.example), NULL) AS card_collocations
            FROM
                cards
            JOIN
                words ON cards.word_id = words.id
            LEFT JOIN
                in_context ON cards.word_id = in_context.word_id
            LEFT JOIN
                collocations ON cards.word_id = collocations.word_id
            WHERE
                cards.id = :cardId -- Specify the card ID to retrieve
            GROUP BY
                cards.id, words.id
            """;

    private static final String GET_CARDS_FOR_EXERCISE = """
            SELECT
                c.id AS card_id,
                vhw.vocab_id,
                vhw.word_ref as word_id,
                c.score,
                c.last_update_time,
                w.word,
                w.part_of_speech,
                w.transcription,
                w.meaning,
                array_remove(
                    array_agg(DISTINCT 'example:' || in_context.example || ';' || 'matchedWords:' || in_context.matched_words),
                    NULL
                ) AS exercise_sentences
            FROM vocab_has_words vhw
                JOIN words w ON vhw.word_ref = w.id
                LEFT JOIN in_context ON vhw.word_ref = in_context.word_id
                LEFT JOIN cards c ON vhw.word_ref = c.word_id AND vhw.vocab_id = c.vocab_id AND c.user_id = :userId
            WHERE vhw.vocab_id = :vocabId AND (c.status != 'LEARNED' OR c.status IS NULL)
            GROUP BY vhw.vocab_id, vhw.word_ref, w.id, c.id, c.score
            ORDER BY c.score
            LIMIT :limit;
    """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public CardHeadlineDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Card> getCards(String userId, int vocabId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        parameters.addValue("vocabId", vocabId);
        return jdbcTemplate.query(ALL_JOINS_QUERY, parameters, new FullCardRowMapper());
    }

    public Card getCardById(int cardId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("cardId", cardId);
        return jdbcTemplate.queryForObject(GET_CARD_HEADLINE, parameters, new FullCardRowMapper());
    }

    public List<Exercise> getCardsForExercise(String userId, int vocabId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        parameters.addValue("vocabId", vocabId);
        parameters.addValue("limit", limit);
        return jdbcTemplate.query(GET_CARDS_FOR_EXERCISE, parameters, new ExerciseRowMapper());
    }

    private static class FullCardRowMapper implements RowMapper<Card> {

        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer cardId = rs.getInt("card_id");
            Integer vocabId = rs.getInt("vocab_id");
            String status = rs.getString("status");
            int score = rs.getInt("score");
            Integer wordId = rs.getInt("word_id");

            Card cardData = new Card();
            cardData.setId(cardId);
            cardData.setVocabId(vocabId);
            cardData.setStatus(StringUtils.hasText(status) ? CardStatus.valueOf(status) : CardStatus.UNSEEN);
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

            word.setStrSentences(sentences);
            word.setCollocations(collocations);

            return cardData;
        }
    }

    private static class ExerciseRowMapper implements RowMapper<Exercise> {
        @Override
        public Exercise mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer cardId = rs.getInt("card_id");
            Integer wordId = rs.getInt("word_id");

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
                int wordId = rs.getInt("word_id");
                String example = rs.getString("example");
                String matchedWords = rs.getString("matched_words");
                Sentence sentence = new Sentence(example, wordId)
                        .withMatchedWords(matchedWords);
                result.add(sentence);
            }
            return result
                    .stream()
                    .collect(Collectors.groupingBy(Sentence::getWordId));
        }
    }

}
