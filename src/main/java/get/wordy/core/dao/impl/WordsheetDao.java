package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class WordsheetDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public WordsheetDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WordsheetHeader> selectAllByClassId(String classId) {
        String query = """
                SELECT ws.id AS wordsheet_id,
                       ws.name,
                       ws.is_shared,
                       COUNT(refs.word_ref) AS words_total
                FROM word_sheet ws
                LEFT JOIN wordsheet_to_words refs ON ws.id = refs.wordsheet_id
                WHERE ws.class_id = :classId
                GROUP BY ws.id, ws.name, ws.is_shared
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("classId", classId);

        return jdbcTemplate.query(query, params, (rs, rowNum) -> new WordsheetHeader(
                rs.getInt("wordsheet_id"),
                rs.getString("name"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        ));
    }

    public WordsheetHeader insert(String classId, String name) {
        String query = """
                INSERT INTO word_sheet (class_id, name, is_shared, create_time)
                VALUES (:classId, :name, false, current_timestamp)
                RETURNING id AS wordsheet_id, name, is_shared, 0 AS words_total
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("name", name);

        return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> new WordsheetHeader(
                rs.getInt("wordsheet_id"),
                rs.getString("name"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        ));
    }

    public WordsheetHeader rename(int wordsheetId, String name) {
        String query = """
                UPDATE word_sheet
                SET name = :name
                WHERE id = :wordsheetId
                RETURNING id AS wordsheet_id, name, is_shared, 0 AS words_total
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wordsheetId", wordsheetId)
                .addValue("name", name);

        return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> new WordsheetHeader(
                rs.getInt("wordsheet_id"),
                rs.getString("name"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        ));
    }

    public WordsheetHeader setIsShared(int wordsheetId, boolean isShared) {
        String query = """
                UPDATE word_sheet
                SET is_shared = :isShared
                WHERE id = :wordsheetId
                RETURNING id AS wordsheet_id, name, is_shared, 0 AS words_total
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wordsheetId", wordsheetId)
                .addValue("isShared", isShared);

        return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> new WordsheetHeader(
                rs.getInt("wordsheet_id"),
                rs.getString("name"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        ));
    }

    public WordsheetHeader selectById(int wordsheetId) {
        String query = """
                SELECT ws.id AS wordsheet_id,
                       ws.name,
                       ws.is_shared,
                       COUNT(refs.word_ref) AS words_total
                FROM word_sheet ws
                LEFT JOIN wordsheet_to_words refs ON ws.id = refs.wordsheet_id
                WHERE ws.id = :wordsheetId
                GROUP BY ws.id, ws.name, ws.is_shared
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("wordsheetId", wordsheetId);

        return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> new WordsheetHeader(
                rs.getInt("wordsheet_id"),
                rs.getString("name"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        ));
    }

    public void addToWordsheet(int wordsheetId, Set<Integer> wordsRefs) {
        String query = """
                INSERT INTO wordsheet_to_words (wordsheet_id, word_ref)
                VALUES (:wordsheetId, :wordRef)
                """;
        jdbcTemplate.batchUpdate(query, wordsRefs.stream()
                .map(id -> new MapSqlParameterSource()
                        .addValue("wordsheetId", wordsheetId)
                        .addValue("wordRef", id))
                .toArray(MapSqlParameterSource[]::new));
    }

    public void removeFromWordsheet(int wordsheetId, Set<Integer> wordsRefs) {
        String query = """
                DELETE FROM wordsheet_to_words
                WHERE wordsheet_id = :wordsheetId AND word_ref = :wordRef
                """;
        jdbcTemplate.batchUpdate(query, wordsRefs.stream()
                .map(ref -> new MapSqlParameterSource()
                        .addValue("wordsheetId", wordsheetId)
                        .addValue("wordRef", ref))
                .toArray(MapSqlParameterSource[]::new));
    }

    public Set<Integer> getWordsRefs(int wordsheetId) {
        String query = """
                SELECT word_ref
                FROM wordsheet_to_words
                WHERE wordsheet_id = :wordsheetId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("wordsheetId", wordsheetId);
        return jdbcTemplate.queryForStream(query, params, (rs, rowNum) -> rs.getInt("word_ref"))
                .collect(Collectors.toSet());
    }

    public int deleteWordsheetById(int wordsheetId) {
        // delete from related tables first
        String deleteWordsRefs = "DELETE FROM wordsheet_to_words WHERE wordsheet_id = :wordsheetId";
        jdbcTemplate.update(deleteWordsRefs, new MapSqlParameterSource("wordsheetId", wordsheetId));

        // delete the wordsheet itself
        String deleteWordsheet = "DELETE FROM word_sheet WHERE id = :wordsheetId";

        return jdbcTemplate.update(deleteWordsheet, new MapSqlParameterSource("wordsheetId", wordsheetId));
    }

}
