package get.wordy.core.dao.impl;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Collocation;
import get.wordy.core.bean.Context;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.CloseUtils;
import get.wordy.core.db.ConnectionWrapper;

import java.sql.*;
import java.util.*;

public class CardDao extends BaseDao<Card> {

    public static final String INSERT_SQL = """
            INSERT INTO cards (status, update_time, dictionary_id, word_id) VALUES (?,?,?,?)
            """;
    public static final String GENERATE_EMPTY_CARDS_QUERY = """
            INSERT INTO cards (dictionary_id, word_id) VALUES (?,?)
            """;
    public static final String DELETE_SQL = "DELETE FROM cards WHERE id=?";
    public static final String UPDATE_SQL = """
            UPDATE cards SET status=?,score=?,word_id=?,dictionary_id=?,update_time=NOW() WHERE id=?
            """;
    public static final String SELECT_ALL_CARDS = """
            SELECT cards.id, status, score, create_time, update_time, word_id, dictionary_id FROM cards
            JOIN words ON cards.word_id = words.id ORDER BY %s ASC
            """;
    private static final String RESET_SCORE_SQL = "UPDATE cards SET score=?, status=? WHERE dictionary_id=?";
    private static final String UPDATE_STATUS = """
            UPDATE cards SET status=?,score=?,update_time=NOW() WHERE id=?
            """;
    private static final String COUNT_SCORE_SQL = """
            SELECT status, COUNT(status) FROM cards WHERE dictionary_id=? GROUP BY status
            """;
    private static final String SELECT_FOR_EXERCISE_SQL = """
            SELECT id FROM cards WHERE dictionary_id=? AND status=? ORDER BY create_time LIMIT ?
            """;
    private static final String SELECT_ALL_CARDS_BY_DIC = "SELECT * FROM cards WHERE dictionary_id=? ORDER BY create_time ASC";

    CardDao(ConnectionWrapper connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Card insert(Card card) throws DaoException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT_SQL)) {
            CardStatus status = card.getStatus();
            statement.setString(1, status != null ? status.name() : null);
            statement.setTimestamp(2, Timestamp.from(card.getInsertedAt()));
            statement.setInt(3, card.getDictionaryId());
            statement.setInt(4, card.getWordId());
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int cardId = resultSet.getInt(1);
                insertContexts(card.getContexts());
                insertCollocations(card.getCollocations());
                card.setId(cardId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting card entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return card;
    }

    public Set<Integer> generateEmptyCards(int dictionaryId, Set<Integer> wordIds) throws DaoException {
        try (PreparedStatement statement = prepareInsert(GENERATE_EMPTY_CARDS_QUERY)) {
            for (Integer wordId : wordIds) {
                statement.setInt(1, dictionaryId);
                statement.setInt(2, wordId);
                statement.addBatch();
            }
            statement.executeBatch();
            // get last inserted id
            ResultSet keys = statement.getGeneratedKeys();
            Set<Integer> ids = new HashSet<>();
            while (keys.next()) {
                ids.add(keys.getInt(1));
            }
            return ids;
        } catch (SQLException ex) {
            throw new DaoException("Error while generating card entities", ex);
        }
    }

    private void insertContexts(List<Context> contexts) throws DaoException {
        for (Context context : contexts) {
            Context inserted = insertContext(context);
            context.setId(inserted.getId());
        }
    }

    private Context insertContext(Context context) throws DaoException {
        String INSERT = "INSERT INTO "
                + "context" + " ("
                + "word_id" + ", "
                + "example"
                + ") VALUES (?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setInt(1, context.getWordId());
            statement.setString(2, context.getExample());
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int contextId = resultSet.getInt(1);
                return context.withId(contextId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting context entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return context;
    }

    private void insertCollocations(List<Collocation> collocations) throws DaoException {
        for (Collocation collocation : collocations) {
            Collocation inserted = insertCollocation(collocation);
            collocation.setId(inserted.getId());
        }
    }

    private Collocation insertCollocation(Collocation collocation) throws DaoException {
        String INSERT = "INSERT INTO "
                + "collocations" + " ("
                + "word_id" + ", "
                + "example"
                + ") VALUES (?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setInt(1, collocation.getWordId());
            statement.setString(2, collocation.getExample());
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int collocationId = resultSet.getInt(1);
                return collocation.withId(collocationId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting collocation entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return collocation;
    }

    @Override
    public void delete(Card card) throws DaoException {
        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(DELETE_SQL);
            statement.setInt(1, card.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting card entity", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
    }

    @Override
    public Card update(Card card) throws DaoException {
        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(UPDATE_SQL);
            statement.setString(1, card.getStatus().name());
            statement.setInt(2, card.getScore());
            statement.setInt(3, card.getWordId());
            statement.setInt(4, card.getDictionaryId());
            statement.setInt(5, card.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating card entity", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }

        deleteFromContexts(card);
        deleteFromCollocations(card);
        insertContexts(card.getContexts());
        insertCollocations(card.getCollocations());
        return card;
    }

    private void deleteFromContexts(Card card) throws DaoException {
        String SQL = "DELETE FROM " + "context" + " WHERE " + "word_id" + "=?";
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting context entity", ex);
        }
    }

    private void deleteFromCollocations(Card card) throws DaoException {
        String SQL = "DELETE FROM " + "collocations" + " WHERE " + "word_id" + "=?";
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting collocation entity", ex);
        }
    }

    public List<Card> selectCardsForDictionary(Dictionary dictionary) throws DaoException {
        ResultSet resultSet = null;
        ArrayList<Card> data = new ArrayList<>();
        try (PreparedStatement statement = prepare(SELECT_ALL_CARDS_BY_DIC)) {
            statement.setInt(1, dictionary.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Card card = new Card();
                card.setId(resultSet.getInt("id"));
                card.setStatus(CardStatus.valueOf(resultSet.getString("status")));
                card.setScore(resultSet.getInt("score"));
                Timestamp createTime = resultSet.getTimestamp("create_time");
                if (createTime != null) {
                    card.setInsertedAt(createTime.toInstant());
                }
                Timestamp updateTime = resultSet.getTimestamp("update_time");
                if (updateTime != null) {
                    card.setUpdatedAt(updateTime.toInstant());
                }
                card.setWordId(resultSet.getInt("word_id"));
                card.setDictionaryId(resultSet.getInt("dictionary_id"));
                data.add(card);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card entities for current dictionary", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return data;
    }

    public List<Card> selectAllCardsSortedBy(String fieldName) throws DaoException {
        ResultSet resultSet = null;
        ArrayList<Card> data = new ArrayList<>();
        String query = String.format(SELECT_ALL_CARDS, fieldName);
        try (PreparedStatement statement = prepare(query)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Card card = new Card();
                card.setId(resultSet.getInt("id"));
                card.setStatus(CardStatus.valueOf(resultSet.getString("status")));
                card.setScore(resultSet.getInt("score"));
                Timestamp createTime = resultSet.getTimestamp("create_time");
                if (createTime != null) {
                    card.setInsertedAt(createTime.toInstant());
                }
                Timestamp updateTime = resultSet.getTimestamp("update_time");
                if (updateTime != null) {
                    card.setUpdatedAt(updateTime.toInstant());
                }
                card.setWordId(resultSet.getInt("word_id"));
                card.setDictionaryId(resultSet.getInt("dictionary_id"));
                data.add(card);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card entities sorted by", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return data;
    }

    public int[] selectCardIdsForExercise(int dictionaryId, int limit) throws DaoException {

        int[] buffer = new int[limit]; // initial array with the largest possible capacity;
        int cnt = 0; // retrieved amount

        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SELECT_FOR_EXERCISE_SQL)) {
            statement.setInt(1, dictionaryId);
            statement.setString(2, CardStatus.TO_LEARN.name());
            statement.setInt(3, limit);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                buffer[cnt] = id;
                cnt++;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card entities for exercise", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        // downsize the array if retrieved amount is less than the limit
        if (cnt < limit) {
            int[] ids = new int[cnt];
            System.arraycopy(buffer, 0, ids, 0, cnt);
            return ids;
        }
        return buffer;
    }

    public List<Context> getContextsFor(Card card) throws DaoException {
        String SQL = "SELECT * FROM " + "context" + " WHERE " + "word_id" + "=?";
        ArrayList<Context> contexts = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String example = resultSet.getString(2);
                int wordId = resultSet.getInt(3);
                Context context = new Context(id, example, wordId);
                contexts.add(context);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving context entities for word=" + card.getWordId(), ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return contexts;
    }

    public List<Collocation> getCollocationsFor(Card card) throws DaoException {
        ArrayList<Collocation> collocations = new ArrayList<>();
        String SQL = "SELECT * FROM " + "collocations" + " WHERE " + "word_id" + "=?";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String example = resultSet.getString(2);
                int wordId = resultSet.getInt(3);
                Collocation collocation = new Collocation(id, example, wordId);
                collocations.add(collocation);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while selecting collocation entities for word=" + card.getWordId(), ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return collocations;
    }

    public Map<String, Integer> getScore(Dictionary dictionary) throws DaoException {
        ResultSet resultSet = null;
        Map<String, Integer> statuses = new HashMap<>();
        try (PreparedStatement statement = prepare(COUNT_SCORE_SQL)) {
            statement.setInt(1, dictionary.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                int count = resultSet.getInt(2);
                statuses.put(status, count);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving score for dictionary", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return statuses;
    }

    public void resetScore(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepare(RESET_SCORE_SQL)) {
            statement.setInt(1, 0);
            statement.setString(2, CardStatus.DEFAULT_STATUS.name());
            statement.setInt(3, dictionary.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while resetting score for dictionary", ex);
        }
    }

    public void updateStatus(int id, CardStatus status, int score) throws DaoException {
        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(UPDATE_STATUS);
            statement.setString(1, status.name());
            statement.setInt(2, score);
            statement.setInt(3, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating status and score", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
    }

}