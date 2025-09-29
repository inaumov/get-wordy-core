package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.id.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class ProgressDao {

    private static final String INSERT_PROGRESS_QUERY = """
            INSERT INTO progress (vocab_id, word_id, status, user_id) VALUES (?, ?, ?, ?)
            """;
    private static final String DELETE_PROGRESS_QUERY = """
            DELETE FROM progress WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String GET_PROGRESS_QUERY = """
            SELECT * FROM progress WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String UPDATE_PROGRESS_QUERY = """
            UPDATE progress SET status=?, score=?, last_update_time=NOW() WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String PROGRESS_SUMMARY_QUERY = """
            SELECT status, COUNT(status) FROM progress WHERE vocab_id=? AND user_id=? GROUP BY status
            """;
    private static final String GET_PROGRESS_IN_VOCAB_QUERY = """
            SELECT * FROM progress WHERE vocab_id=? AND user_id=?
            """;
    private static final String PICK_CARD_FOR_EXERCISE_QUERY = """
            SELECT * FROM progress
            WHERE vocab_id=? AND user_id=?
              AND (status != 'LEARNED' OR status IS NULL)
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProgressDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Progress addRecord(OwnerId ownerId, Progress progress) {
        jdbcTemplate.update(
                INSERT_PROGRESS_QUERY,
                progress.getVocabId(),
                progress.getWordId(),
                progress.getStatus() != null ? progress.getStatus().name() : null,
                ownerId.ownerId()
        );
        return progress;
    }

    public void addRecords(OwnerId ownerId, List<Progress> records) {
        jdbcTemplate.batchUpdate(
                INSERT_PROGRESS_QUERY,
                records,
                records.size(),
                (ps, progress) -> {
                    ps.setInt(1, progress.getVocabId());
                    ps.setInt(2, progress.getWordId());
                    ps.setString(3, progress.getStatus() != null ? progress.getStatus().name() : null);
                    ps.setString(4, ownerId.ownerId());
                }
        );
    }

    public void delete(OwnerId ownerId, int vocabId, int wordId) {
        jdbcTemplate.update(DELETE_PROGRESS_QUERY, vocabId, wordId, ownerId.ownerId());
    }

    public List<Progress> selectByWordIds(OwnerId ownerId, int vocabId, int... wordIds) {
        if (wordIds.length == 0) {
            return jdbcTemplate.query(GET_PROGRESS_IN_VOCAB_QUERY, new ProgressRowMapper(), vocabId, ownerId.ownerId());
        }

        String placeholders = String.join(",", Collections.nCopies(wordIds.length, "?"));
        String query = "SELECT * FROM progress WHERE vocab_id=? AND user_id=? AND word_id IN (" + placeholders + ")";
        Object[] params = new Object[2 + wordIds.length];
        params[0] = vocabId;
        params[1] = ownerId.ownerId();
        for (int i = 0; i < wordIds.length; i++) {
            params[2 + i] = wordIds[i];
        }
        return jdbcTemplate.query(query, new ProgressRowMapper(), params);
    }

    public List<Progress> pickForExercise(OwnerId ownerId, int vocabId, int limit) {
        return jdbcTemplate.query(PICK_CARD_FOR_EXERCISE_QUERY, new ProgressRowMapper(), vocabId, ownerId.ownerId(), limit);
    }

    public Progress selectById(OwnerId ownerId, int vocabId, int wordId) {
        List<Progress> results = jdbcTemplate.query(GET_PROGRESS_QUERY, new ProgressRowMapper(), vocabId, wordId, ownerId.ownerId());
        return results.isEmpty() ? null : results.getFirst();
    }

    public Map<String, Integer> getProgressSummary(OwnerId ownerId, int vocabId) {
        return jdbcTemplate.query(PROGRESS_SUMMARY_QUERY,
                rs -> {
                    Map<String, Integer> map = new HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getString("status"), rs.getInt(2));
                    }
                    return map;
                },
                vocabId, ownerId.ownerId()
        );
    }

    public int updateProgress(OwnerId ownerId, Progress progress) {
        return jdbcTemplate.update(
                UPDATE_PROGRESS_QUERY,
                progress.getStatus().name(),
                progress.getScore(),
                progress.getVocabId(),
                progress.getWordId(),
                ownerId.ownerId()
        );
    }

    public void batchUpsertProgress(OwnerId ownerId, List<Progress> records) {

        jdbcTemplate.batchUpdate(
                UPDATE_PROGRESS_QUERY,
                records,
                records.size(),
                (ps, progress) -> {
                    ps.setString(1, progress.getStatus().name());
                    ps.setInt(2, progress.getScore());
                    ps.setInt(3, progress.getVocabId());
                    ps.setInt(4, progress.getWordId());
                    ps.setString(5, ownerId.ownerId());
                }
        );
    }

    private static class ProgressRowMapper implements RowMapper<Progress> {
        @Override
        public Progress mapRow(ResultSet rs, int rowNum) throws SQLException {
            Progress p = new Progress();
            String status = rs.getString("status");
            if (status != null) {
                p.setStatus(CardStatus.valueOf(status));
            }
            p.setScore(rs.getInt("score"));
            Timestamp createTime = rs.getTimestamp("create_time");
            if (createTime != null) {
                p.setInsertedAt(createTime.toInstant());
            }
            Timestamp updateTime = rs.getTimestamp("last_update_time");
            if (updateTime != null) {
                p.setUpdatedAt(updateTime.toInstant());
            }
            p.setWordId(rs.getInt("word_id"));
            p.setVocabId(rs.getInt("vocab_id"));
            return p;
        }
    }

}