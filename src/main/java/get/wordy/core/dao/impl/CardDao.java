package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.*;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.LocalTxManager;

import java.sql.*;
import java.util.*;

public class CardDao extends BaseDao<Card> {

    private static final String INSERT_CARD_QUERY = """
            INSERT INTO cards (vocab_id, word_id, status, user_id) VALUES (?,?,?,?)
            """;
    private static final String DELETE_CARD_QUERY = "DELETE FROM cards WHERE id=? AND user_id=?";

    private static final String SELECT_CARD_QUERY = """
            SELECT * FROM cards WHERE id=?
            """;
    private static final String UPDATE_STATUS_QUERY = """
            UPDATE cards SET status=?, last_update_time=NOW() WHERE id=?
            """;
    private static final String UPDATE_SCORE_QUERY = """
            UPDATE cards SET score=?, last_update_time=NOW() WHERE id=?
            """;
    private static final String SCORE_SUMMARY_QUERY = """
            SELECT status, COUNT(status) FROM cards WHERE vocab_id=? AND user_id=? GROUP BY status
            """;
    private static final String SELECT_FOR_EXERCISE_QUERY = """
            SELECT id FROM cards WHERE vocab_id=? AND user_id=? AND status=? ORDER BY create_time LIMIT ?
            """;
    private static final String CARDS_IN_PROGRESS_QUERY = """
            SELECT * FROM cards WHERE vocab_id=? AND user_id=? ORDER BY create_time ASC
            """;

    CardDao(LocalTxManager txManager) {
        super(txManager);
    }

    public Card insert(OwnerId ownerId, Card card) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_CARD_QUERY)) {
            CardStatus status = card.getStatus();
            statement.setInt(1, card.getVocabId());
            statement.setInt(2, card.getWordId());
            statement.setString(3, status != null ? status.name() : null);
            statement.setString(4, ownerId.ownerId());
            statement.execute();
            // get last inserted id
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int cardId = resultSet.getInt(1);
                card.setId(cardId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting a card record", ex);
        }
        return card;
    }

    public void addCards(OwnerId ownerId, int vocabId, Set<Integer> wordIds) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_CARD_QUERY)) {
            for (Integer wordId : wordIds) {
                statement.setInt(1, vocabId);
                statement.setInt(2, wordId);
                statement.setString(3, CardStatus.DEFAULT_STATUS.name());
                statement.setString(4, ownerId.ownerId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while generating new cards", ex);
        }
    }

    public void delete(OwnerId ownerId, int cardId) throws DaoException {
        try (var statement = prepareStatement(DELETE_CARD_QUERY)) {
            statement.setInt(1, cardId);
            statement.setString(2, ownerId.ownerId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting a card record", ex);
        }
    }

    public List<Card> selectCards(OwnerId ownerId, int vocabId) throws DaoException {
        ArrayList<Card> data = new ArrayList<>();
        try (var statement = prepareStatement(CARDS_IN_PROGRESS_QUERY)) {
            statement.setInt(1, vocabId);
            statement.setString(2, ownerId.ownerId());
            ResultSet resultSet = statement.executeQuery();
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

    public Card selectById(int cardId) throws DaoException {
        try (var statement = prepareStatement(SELECT_CARD_QUERY)) {
            statement.setInt(1, cardId);
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
        destination.setId(resultSet.getInt("id"));
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

    public int[] selectCardIdsForExercise(OwnerId ownerId, int vocabId, int limit) throws DaoException {

        int[] buffer = new int[limit]; // initial array with the largest possible capacity;
        int cnt = 0; // retrieved amount

        try (var statement = prepareStatement(SELECT_FOR_EXERCISE_QUERY)) {
            statement.setInt(1, vocabId);
            statement.setString(2, ownerId.ownerId());
            statement.setString(3, CardStatus.TO_LEARN.name());
            statement.setInt(4, limit);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                buffer[cnt] = id;
                cnt++;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card records for exercise", ex);
        }
        // downsize the array if retrieved amount is less than the limit
        if (cnt < limit) {
            int[] ids = new int[cnt];
            System.arraycopy(buffer, 0, ids, 0, cnt);
            return ids;
        }
        return buffer;
    }

    public Map<String, Integer> getScoreSummary(OwnerId ownerId, int vocabId) throws DaoException {
        Map<String, Integer> statuses = new HashMap<>();
        try (var statement = prepareStatement(SCORE_SUMMARY_QUERY)) {
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

    public int updateStatus(int cardId, CardStatus status) throws DaoException {
        try (var statement = prepareStatement(UPDATE_STATUS_QUERY)) {
            statement.setString(1, status.name());
            statement.setInt(2, cardId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating status", ex);
        }
    }

    public int updateScore(int cardId, int score) throws DaoException {
        try (var statement = prepareStatement(UPDATE_SCORE_QUERY)) {
            statement.setInt(1, score);
            statement.setInt(2, cardId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating score", ex);
        }
    }

    public void batchUpdateStatuses(List<Card> cards) throws DaoException {
        try (var statement = prepareStatement(UPDATE_STATUS_QUERY)) {
            for (Card card : cards) {
                statement.setString(1, card.getStatus().name());
                statement.setInt(2, card.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating statuses in batch", ex);
        }
    }

    public void batchUpdateScores(List<Card> cards) throws DaoException {
        try (var statement = prepareStatement(UPDATE_SCORE_QUERY)) {
            for (Card card : cards) {
                statement.setInt(1, card.getScore());
                statement.setInt(2, card.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating scores in batch", ex);
        }
    }

}