package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.LocalTxManager;

import java.sql.*;
import java.util.*;

public class ProgressDao extends BaseDao<Card> {

    private static final String INSERT_PROGRESS_QUERY = """
            INSERT INTO cards (vocab_id, word_id, status, user_id) VALUES (?,?,?,?)
            """;
    private static final String DELETE_PROGRESS_QUERY = """
            DELETE FROM cards WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String GET_PROGRESS_QUERY = """
            SELECT * FROM cards WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String UPDATE_PROGRESS_QUERY = """
            UPDATE cards SET status=?, score=?, last_update_time=NOW() WHERE vocab_id=? AND word_id=? AND user_id=?
            """;
    private static final String PROGRESS_SUMMARY_QUERY = """
            SELECT status, COUNT(status) FROM cards WHERE vocab_id=? AND user_id=? GROUP BY status
            """;
    private static final String GET_PROGRESS_IN_VOCAB_QUERY = """
            SELECT * FROM cards WHERE vocab_id=? AND user_id=?
            """;
    private static final String GET_PROGRESS_BY_WORD_REFS_QUERY = """
            SELECT * FROM cards WHERE vocab_id=? AND user_id=? AND word_id IN (%s)
            """;

    ProgressDao(LocalTxManager txManager) {
        super(txManager);
    }

    public Card insert(OwnerId ownerId, Card card) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_PROGRESS_QUERY)) {
            CardStatus status = card.getStatus();
            statement.setInt(1, card.getVocabId());
            statement.setInt(2, card.getWordId());
            statement.setString(3, status != null ? status.name() : null);
            statement.setString(4, ownerId.ownerId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting a card record", ex);
        }
        return card;
    }

    public void addCards(OwnerId ownerId, List<Card> cards) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_PROGRESS_QUERY)) {
            for (Card card : cards) {
                CardStatus status = card.getStatus();
                statement.setInt(1, card.getVocabId());
                statement.setInt(2, card.getWordId());
                statement.setString(3, status != null ? status.name() : null);
                statement.setString(4, ownerId.ownerId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while generating new cards", ex);
        }
    }

    public void delete(OwnerId ownerId, int vocabId, int wordId) throws DaoException {
        try (var statement = prepareStatement(DELETE_PROGRESS_QUERY)) {
            statement.setInt(1, vocabId);
            statement.setInt(2, wordId);
            statement.setString(3, ownerId.ownerId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting a card record", ex);
        }
    }

    public List<Card> selectCards(OwnerId ownerId, int vocabId, int... wordRefs) throws DaoException {
        // generate the dynamic query
        String selectInQuery;
        if (wordRefs.length == 0) {
            selectInQuery = GET_PROGRESS_IN_VOCAB_QUERY;
        } else {
            String placeholders = String.join(",", Collections.nCopies(wordRefs.length, "?"));
            selectInQuery = String.format(GET_PROGRESS_BY_WORD_REFS_QUERY, placeholders);
        }

        ArrayList<Card> data = new ArrayList<>();
        try (var preparedStatement = prepareStatement(selectInQuery)) {
            preparedStatement.setInt(1, vocabId);
            preparedStatement.setString(2, ownerId.ownerId());
            // bind parameters
            int index = 3;
            for (Integer id : wordRefs) {
                preparedStatement.setInt(index++, id);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Card card = new Card();
                mapResultSetToCardEntity(resultSet, card);
                data.add(card);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card records for vocabulary id", ex);
        }
        return data;
    }

    public Card selectById(OwnerId ownerId, int vocabId, int wordId) throws DaoException {
        try (var statement = prepareStatement(GET_PROGRESS_QUERY)) {
            statement.setInt(1, vocabId);
            statement.setInt(2, wordId);
            statement.setString(3, ownerId.ownerId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Card card = new Card();
                mapResultSetToCardEntity(resultSet, card);
                return card;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving a card record", ex);
        }
        return null;
    }

    private void mapResultSetToCardEntity(ResultSet resultSet, Card destination) throws SQLException {
        destination.setStatus(CardStatus.valueOf(resultSet.getString("status")));
        destination.setScore(resultSet.getInt("score"));
        Timestamp createTime = resultSet.getTimestamp("create_time");
        if (createTime != null) {
            destination.setInsertedAt(createTime.toInstant());
        }
        Timestamp updateTime = resultSet.getTimestamp("last_update_time");
        if (updateTime != null) {
            destination.setUpdatedAt(updateTime.toInstant());
        }
        destination.setWordId(resultSet.getInt("word_id"));
        destination.setVocabId(resultSet.getInt("vocab_id"));
    }

    public Map<String, Integer> getProgressSummary(OwnerId ownerId, int vocabId) throws DaoException {
        Map<String, Integer> statuses = new HashMap<>();
        try (var statement = prepareStatement(PROGRESS_SUMMARY_QUERY)) {
            statement.setInt(1, vocabId);
            statement.setString(2, ownerId.ownerId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                int count = resultSet.getInt(2);
                statuses.put(status, count);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving score summary for vocabulary", ex);
        }
        return statuses;
    }

    public int updateProgress(OwnerId ownerId, Card card) throws DaoException {
        try (var statement = prepareStatement(UPDATE_PROGRESS_QUERY)) {
            statement.setString(1, card.getStatus().name());
            statement.setInt(2, card.getScore());
            statement.setInt(3, card.getVocabId());
            statement.setInt(4, card.getWordId());
            statement.setString(5, ownerId.ownerId());
            int i = statement.executeUpdate();
            if (i > 1) {
                throw new DaoException("Invalid database state while updating progress");
            }
            return i;
        } catch (SQLException ex) {
            throw new DaoException("Error while updating progress", ex);
        }
    }

    public void batchUpsertProgress(OwnerId ownerId, List<Card> cards) throws DaoException {
        final String UPDATE_PROGRESS_QUERY = """
                    UPDATE cards
                    SET score=?, status=?
                    WHERE vocab_id=? AND word_id=? AND user_id=?
                """;

        try (var statement = prepareStatement(UPDATE_PROGRESS_QUERY)) {
            for (Card card : cards) {
                statement.setInt(1, card.getScore());
                statement.setString(2, card.getStatus().name());
                statement.setInt(3, card.getVocabId());
                statement.setInt(4, card.getWordId());
                statement.setString(5, ownerId.ownerId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating progress in batch", ex);
        }
    }

}