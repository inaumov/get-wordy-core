package get.wordy.core.dao.impl;

import get.wordy.core.api.db.ColumnNames;
import get.wordy.core.api.db.TableNames;
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

public class CardDao extends BaseDao<Card> implements ColumnNames, TableNames {

    public static final String INSERT_SQL = "";
    public static final String DELETE_SQL = "";
    public static final String UPDATE_SQL = "";
    public static final String SELECT_ALL_CARDS = """
            SELECT cards.id, status, rating, create_time, update_time, word_id, dictionary_id FROM cards
            JOIN words ON cards.word_id = words.id ORDER BY %s DESC
            """;

    CardDao(ConnectionWrapper connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Card insert(Card card) throws DaoException {

        String query = "INSERT INTO " +
                TBL_CARDS +
                "(" +
                STATUS +
                "," +
                UPDATE_TIME +
                "," +
                DIC_ID +
                "," +
                WORD_ID +
                ") VALUES (?,?,?,?)";

        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(query)) {
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

    public boolean generateCardsWithoutDefinitions(Set<Integer> wordIds, int dictionaryId, CardStatus status) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(TBL_CARDS);
        query.append("(");
        query.append(STATUS);
        query.append(",");
        query.append(UPDATE_TIME);
        query.append(",");
        query.append(DIC_ID);
        query.append(",");
        query.append(WORD_ID);
        query.append(")VALUES(?,NOW(),?,?)");

        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(query.toString())) {
            for (Integer wordId : wordIds) {
                statement.setString(1, status != null ? status.name() : null);
                statement.setInt(2, dictionaryId);
                statement.setInt(3, wordId);
                statement.addBatch();
            }
            return statement.executeBatch().length == wordIds.size();
        } catch (SQLException ex) {
            throw new DaoException("Error while generating card entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
    }

    private void insertContexts(ArrayList<Context> contexts) throws DaoException {
        for (Context context : contexts) {
            contexts.remove(context);
            Context inserted = insertContext(context);
            contexts.add(inserted);
        }
    }

    private Context insertContext(Context context) throws DaoException {
        String INSERT = "INSERT INTO "
                + TBL_CONTEXT + " ("
                + WORD_ID + ", "
                + EXAMPLE
                + ") VALUES (?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setInt(1, context.wordId());
            statement.setString(2, context.example());
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

    private void insertCollocations(ArrayList<Collocation> collocations) throws DaoException {
        for (Collocation collocation : collocations) {
            collocations.remove(collocation);
            Collocation inserted = insertCollocation(collocation);
            collocations.add(inserted);
        }
    }

    private Collocation insertCollocation(Collocation collocation) throws DaoException {
        String INSERT = "INSERT INTO "
                + TBL_COLLOCATIONS + " ("
                + WORD_ID + ", "
                + EXAMPLE
                + ") VALUES (?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setInt(1, collocation.wordId());
            statement.setString(2, collocation.example());
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

        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(TBL_CARDS);
        query.append(" WHERE ");
        query.append(ID);
        query.append("=?");

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query.toString());
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

        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        query.append(TBL_CARDS);
        query.append(" SET ");
        query.append(STATUS);
        query.append("=?,");
        query.append(RATING);
        query.append("=?,");
        query.append(WORD_ID);
        query.append("=?,");
        query.append(DIC_ID);
        query.append("=?,");
        query.append(UPDATE_TIME);
        query.append("=NOW() ");
        query.append("WHERE ");
        query.append(ID);
        query.append("=?");

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query.toString());
            statement.setString(1, card.getStatus().name());
            statement.setInt(2, card.getRating());
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
        String SQL = "DELETE FROM " + TBL_CONTEXT + " WHERE " + WORD_ID + "=?";
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting context entity", ex);
        }
    }

    private void deleteFromCollocations(Card card) throws DaoException {
        String SQL = "DELETE FROM " + TBL_COLLOCATIONS + " WHERE " + WORD_ID + "=?";
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getWordId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting collocation entity", ex);
        }
    }

    public List<Card> selectCardsForDictionary(Dictionary dictionary) throws DaoException {
        String SELECT_ALL_CARDS_SQL = "SELECT * FROM " + TBL_CARDS + " WHERE " + DIC_ID + "=?";
        ResultSet resultSet = null;
        ArrayList<Card> data = new ArrayList<Card>();
        try (PreparedStatement statement = prepare(SELECT_ALL_CARDS_SQL)) {
            statement.setInt(1, dictionary.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Card card = new Card();
                card.setId(resultSet.getInt(ID));
                card.setStatus(CardStatus.valueOf(resultSet.getString(STATUS)));
                card.setRating(resultSet.getInt(RATING));
                card.setInsertedAt(resultSet.getTimestamp(CREATE_TIME).toInstant());
                card.setUpdatedAt(resultSet.getTimestamp(UPDATE_TIME).toInstant());
                card.setWordId(resultSet.getInt(WORD_ID));
                card.setDictionaryId(resultSet.getInt(DIC_ID));
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
                card.setId(resultSet.getInt(ID));
                card.setStatus(CardStatus.valueOf(resultSet.getString(STATUS)));
                card.setRating(resultSet.getInt(RATING));
                card.setInsertedAt(resultSet.getTimestamp(CREATE_TIME).toInstant());
                card.setUpdatedAt(resultSet.getTimestamp(UPDATE_TIME).toInstant());
                card.setWordId(resultSet.getInt(WORD_ID));
                card.setDictionaryId(resultSet.getInt(DIC_ID));
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

        String SELECT_FOR_EXERCISE_SQL = "SELECT " + ID + " FROM " + TBL_CARDS +
                " WHERE " + DIC_ID + "=? AND " + STATUS + "=?" + " ORDER BY " + CREATE_TIME + " LIMIT " + limit;
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SELECT_FOR_EXERCISE_SQL)) {
            statement.setInt(1, dictionaryId);
            statement.setString(2, CardStatus.TO_LEARN.name());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(ID);
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
        String SQL = "SELECT * FROM " + TBL_CONTEXT + " WHERE " + WORD_ID + "=?";
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
        String SQL = "SELECT * FROM " + TBL_COLLOCATIONS + " WHERE " + WORD_ID + "=?";
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
        String COUNT_SCORE_SQL = "SELECT " + STATUS + ", COUNT(" + STATUS + ") FROM " + TBL_CARDS +
                " WHERE " + DIC_ID + "=? GROUP BY " + STATUS;
        ResultSet resultSet = null;
        Map<String, Integer> statuses = new HashMap<>();
        try (PreparedStatement statement = prepare(COUNT_SCORE_SQL)) {
            statement.setInt(1, dictionary.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String status = resultSet.getString(STATUS);
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
        String RESET_SCORE_SQL = "UPDATE " + TBL_CARDS + " SET " + RATING + "= ?, " + STATUS + "= ?" + " WHERE " + DIC_ID + "= ?";
        try (PreparedStatement statement = prepare(RESET_SCORE_SQL)) {
            statement.setInt(1, 0);
            statement.setString(2, CardStatus.DEFAULT_STATUS.name());
            statement.setInt(3, dictionary.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while resetting score for dictionary", ex);
        }
    }

    public Card updateStatus(Card card) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        query.append(TBL_CARDS);
        query.append(" SET ");
        query.append(STATUS);
        query.append("=?,");
        query.append(RATING);
        query.append("=?,");
        query.append(UPDATE_TIME);
        query.append("=NOW() ");
        query.append("WHERE ");
        query.append(ID);
        query.append("=?");

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query.toString());
            statement.setString(1, card.getStatus().name());
            statement.setInt(2, card.getRating());
            statement.setInt(3, card.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating status and rating", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }

        return card;
    }

}