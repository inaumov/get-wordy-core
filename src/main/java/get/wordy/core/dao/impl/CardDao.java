package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.Collocation;
import get.wordy.core.api.bean.Context;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.LocalTxManager;

import java.sql.*;
import java.util.*;

public class CardDao extends BaseDao<Card> {

    public static final String INSERT_CARD_QUERY = """
            INSERT INTO cards (dictionary_id, word_id, status) VALUES (?,?,?)
            """;
    public static final String GENERATE_EMPTY_CARDS_QUERY = """
            INSERT INTO cards (dictionary_id, word_id) VALUES (?,?)
            """;
    public static final String DELETE_CARD_QUERY = "DELETE FROM cards WHERE id=?";
    public static final String UPDATE_CARD_QUERY = """
            UPDATE cards SET status=?,score=?,word_id=?,dictionary_id=?,last_update_time=NOW() WHERE id=?
            """;
    public static final String SELECT_CARD_QUERY = """
            SELECT id, status, score, create_time, last_update_time, word_id, dictionary_id FROM cards WHERE id=?
            """;
    private static final String RESET_SCORE_QUERY = """
            UPDATE cards SET score=?, status=?, last_update_time=NOW() WHERE id=?
            """;
    private static final String UPDATE_STATUS_QUERY = """
            UPDATE cards SET status=?,score=?, last_update_time=NOW() WHERE id=?
            """;
    private static final String SCORE_SUMMARY_QUERY = """
            SELECT status, COUNT(status) FROM cards WHERE dictionary_id=? GROUP BY status
            """;
    private static final String SELECT_FOR_EXERCISE_QUERY = """
            SELECT id FROM cards WHERE dictionary_id=? AND status=? ORDER BY create_time LIMIT ?
            """;
    private static final String SELECT_ALL_CARDS_BY_DIC = """
            SELECT * FROM cards WHERE dictionary_id=? ORDER BY create_time ASC
            """;

    // context and collocations
    private static final String INSERT_CONTEXT_QUERY = "INSERT INTO context (card_id, example) VALUES (?,?)";
    private static final String INSERT_COLLOCATIONS_QUERY = "INSERT INTO collocations (card_id, example) VALUES (?,?)";
    private static final String SELECT_CONTEXT_QUERY = "SELECT * FROM context WHERE card_id=?";
    private static final String SELECT_COLLOCATIONS_QUERY = "SELECT * FROM collocations WHERE card_id=?";
    private static final String DELETE_CONTEXT_QUERY = "DELETE FROM context WHERE card_id=?";
    private static final String DELETE_COLLOCATIONS_QUERY = "DELETE FROM collocations WHERE card_id=?";

    CardDao(LocalTxManager txManager) {
        super(txManager);
    }

    @Override
    public Card insert(Card card) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_CARD_QUERY)) {
            CardStatus status = card.getStatus();
            statement.setInt(1, card.getDictionaryId());
            statement.setInt(2, card.getWordId());
            statement.setString(3, status != null ? status.name() : null);
            statement.execute();
            // get last inserted id
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int cardId = resultSet.getInt(1);
                List<Context> contexts = insertContexts(cardId, card.getContexts());
                List<Collocation> collocations = insertCollocations(cardId, card.getCollocations());
                card.setContexts(contexts);
                card.setCollocations(collocations);
                card.setId(cardId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting card headline", ex);
        }
        return card;
    }

    public Set<Integer> generateEmptyCards(int dictionaryId, Set<Integer> wordIds) throws DaoException {
        try (var statement = prepareStatementForInsert(GENERATE_EMPTY_CARDS_QUERY)) {
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

    private List<Context> insertContexts(int cardId, List<Context> contexts) throws DaoException {
        List<Context> copy = new ArrayList<>(contexts.size());
        for (Context context : contexts) {
            Context inserted = insertContext(cardId, context);
            inserted.setCardId(cardId);
            copy.add(inserted);
        }
        return copy;
    }

    private Context insertContext(int cardId, Context context) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_CONTEXT_QUERY)) {
            statement.setInt(1, cardId);
            statement.setString(2, context.getExample());
            statement.execute();
            // get last inserted id
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int contextId = resultSet.getInt(1);
                return context.withId(contextId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting context entity", ex);
        }
        return context;
    }

    private List<Collocation> insertCollocations(int cardId, List<Collocation> collocations) throws DaoException {
        List<Collocation> copy = new ArrayList<>(collocations.size());
        for (Collocation collocation : collocations) {
            Collocation inserted = insertCollocation(cardId, collocation);
            inserted.setCardId(cardId);
            copy.add(inserted);
        }
        return copy;
    }

    private Collocation insertCollocation(int cardId, Collocation collocation) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_COLLOCATIONS_QUERY)) {
            statement.setInt(1, cardId);
            statement.setString(2, collocation.getExample());
            statement.execute();
            // get last inserted id
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int collocationId = resultSet.getInt(1);
                return collocation.withId(collocationId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting collocation entity", ex);
        }
        return collocation;
    }

    @Override
    public void delete(Card card) throws DaoException {
        try (var statement = prepareStatement(DELETE_CARD_QUERY)) {
            statement.setInt(1, card.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting card entity", ex);
        }
    }

    @Override
    public Card update(Card card) throws DaoException {
        try (var statement = prepareStatement(UPDATE_CARD_QUERY)) {
            statement.setString(1, card.getStatus().name());
            statement.setInt(2, card.getScore());
            statement.setInt(3, card.getWordId());
            statement.setInt(4, card.getDictionaryId());
            statement.setInt(5, card.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating card headline", ex);
        }

        deleteFromContexts(card.getId());
        deleteFromCollocations(card.getId());
        List<Context> contexts = insertContexts(card.getId(), card.getContexts());
        List<Collocation> collocations = insertCollocations(card.getId(), card.getCollocations());
        card.setContexts(contexts);
        card.setCollocations(collocations);
        return card;
    }

    private void deleteFromContexts(int cardId) throws DaoException {
        try (var statement = prepareStatement(DELETE_CONTEXT_QUERY)) {
            statement.setInt(1, cardId);
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting context entity", ex);
        }
    }

    private void deleteFromCollocations(int cardId) throws DaoException {
        try (var statement = prepareStatement(DELETE_COLLOCATIONS_QUERY)) {
            statement.setInt(1, cardId);
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting collocation entity", ex);
        }
    }

    public List<Card> selectCardsForDictionary(Dictionary dictionary) throws DaoException {
        ArrayList<Card> data = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_ALL_CARDS_BY_DIC)) {
            statement.setInt(1, dictionary.getId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Card card = new Card();
                mapResultSetToCardEntity(resultSet, card);
                data.add(card);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card entities for current dictionary", ex);
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
            throw new DaoException("Error while retrieving card headline", ex);
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
        destination.setDictionaryId(resultSet.getInt("dictionary_id"));
    }

    public int[] selectCardIdsForExercise(int dictionaryId, int limit) throws DaoException {

        int[] buffer = new int[limit]; // initial array with the largest possible capacity;
        int cnt = 0; // retrieved amount

        try (var statement = prepareStatement(SELECT_FOR_EXERCISE_QUERY)) {
            statement.setInt(1, dictionaryId);
            statement.setString(2, CardStatus.TO_LEARN.name());
            statement.setInt(3, limit);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                buffer[cnt] = id;
                cnt++;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving card entities for exercise", ex);
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
        ArrayList<Context> contexts = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_CONTEXT_QUERY)) {
            statement.setInt(1, card.getId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String example = resultSet.getString(2);
                int wordId = resultSet.getInt(3);
                Context context = new Context(id, example, wordId);
                contexts.add(context);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving context examples for card with id = " + card.getId(), ex);
        }
        return contexts;
    }

    public List<Collocation> getCollocationsFor(Card card) throws DaoException {
        ArrayList<Collocation> collocations = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_COLLOCATIONS_QUERY)) {
            statement.setInt(1, card.getId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String example = resultSet.getString(2);
                int wordId = resultSet.getInt(3);
                Collocation collocation = new Collocation(id, example, wordId);
                collocations.add(collocation);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while selecting collocations for card with id = " + card.getId(), ex);
        }
        return collocations;
    }

    public Map<String, Integer> getScoreSummary(int dictionaryId) throws DaoException {
        Map<String, Integer> statuses = new HashMap<>();
        try (var statement = prepareStatement(SCORE_SUMMARY_QUERY)) {
            statement.setInt(1, dictionaryId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                int count = resultSet.getInt(2);
                statuses.put(status, count);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving score summary for dictionary", ex);
        }
        return statuses;
    }

    public void resetScore(int cardId, CardStatus status) throws DaoException {
        try (var statement = prepareStatement(RESET_SCORE_QUERY)) {
            statement.setInt(1, 0);
            statement.setString(2, status.name());
            statement.setInt(3, cardId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while resetting score for a card", ex);
        }
    }

    public void updateStatus(int cardId, CardStatus status, int score) throws DaoException {
        try (var statement = prepareStatement(UPDATE_STATUS_QUERY)) {
            statement.setString(1, status.name());
            statement.setInt(2, score);
            statement.setInt(3, cardId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating status and score", ex);
        }
    }

    public void batchUpdateScores(List<Card> cards) throws DaoException {
        try (var statement = prepareStatement(UPDATE_STATUS_QUERY)) {
            for (Card card : cards) {
                statement.setString(1, card.getStatus().name());
                statement.setInt(2, card.getScore());
                statement.setInt(3, card.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating statuses and scores in batch", ex);
        }
    }

}