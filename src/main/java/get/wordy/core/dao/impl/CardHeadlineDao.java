package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.dao.impl.helper.SentenceParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Repository
public class CardHeadlineDao {

    private static final String ALL_VOCAB_WORDS_QUERY = """
            SELECT * from vocab_words_headlines WHERE vocab_id = :vocabId
            """;

    private static final String GET_WORD_HEADLINE = """
            SELECT * FROM vocab_words_headlines WHERE vocab_id = :vocabId AND word_id = :wordId
            """;

    private static final String GET_CARDS_FOR_EXERCISE = """
            SELECT
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
                LEFT JOIN progress c ON vhw.word_ref = c.word_id AND vhw.vocab_id = c.vocab_id AND c.user_id = :userId
            WHERE vhw.vocab_id = :vocabId AND (c.status != 'LEARNED' OR c.status IS NULL)
            GROUP BY vhw.vocab_id, vhw.word_ref, w.id, c.score, c.last_update_time
            ORDER BY c.score
            LIMIT :limit;
    """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public CardHeadlineDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Word> getWordsHeadlines(int vocabId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("vocabId", vocabId);
        return jdbcTemplate.query(ALL_VOCAB_WORDS_QUERY, parameters, new WordHeadlineRowMapper());
    }

    public Word getWordHeadlineById(int vocabId, int wordId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("vocabId", vocabId);
        parameters.addValue("wordId", wordId);
        return jdbcTemplate.queryForObject(GET_WORD_HEADLINE, parameters, new WordHeadlineRowMapper());
    }

    public List<Exercise> getCardsForExercise(String userId, int vocabId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        parameters.addValue("vocabId", vocabId);
        parameters.addValue("limit", limit);
        return jdbcTemplate.query(GET_CARDS_FOR_EXERCISE, parameters, new ExerciseRowMapper());
    }

    private static class WordHeadlineRowMapper implements RowMapper<Word> {

        @Override
        public Word mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer wordId = rs.getInt("word_id");

            Word word = new Word(
                    wordId,
                    rs.getString("word"),
                    rs.getString("part_of_speech"),
                    rs.getString("transcription"),
                    rs.getString("meaning"));

            String[] cardSentences = (String[]) rs.getArray("card_sentences").getArray();
            String[] cardCollocations = (String[]) rs.getArray("card_collocations").getArray();

            List<String> sentences = Arrays.asList(cardSentences);
            List<String> collocations = Arrays.asList(cardCollocations);

            word.setStrSentences(sentences);
            word.setCollocations(collocations);

            return word;
        }
    }

    private static class ExerciseRowMapper implements RowMapper<Exercise> {
        @Override
        public Exercise mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer wordId = rs.getInt("word_id");

            Exercise exercise = new Exercise();
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

}
