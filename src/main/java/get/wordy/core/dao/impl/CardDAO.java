package get.wordy.core.dao.impl;

import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.ICardDao;
import get.wordy.core.api.db.ColumnNames;
import get.wordy.core.api.db.IConnectionFactory;
import get.wordy.core.api.db.TableNames;
import get.wordy.core.bean.Card;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Meaning;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.wrapper.GramUnit;
import get.wordy.core.db.CloseUtils;
import get.wordy.core.wrapper.Score;

import java.sql.*;
import java.util.*;

public class CardDao extends BaseDao implements ICardDao, ColumnNames, TableNames {

    public static final String INSERT_SQL = "";
    public static final String DELETE_SQL = "";
    public static final String UPDATE_SQL = "";
    public static final String SELECT_ALL_SQL = "";

    CardDao(IConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void insert(Card card) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(TBL_CARD);
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
            CardStatus status = card.getStatus();
            statement.setString(1, status != null ? status.name() : null);
            statement.setInt(2, card.getDictionaryId());
            statement.setInt(3, card.getWordId());
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int cardId = resultSet.getInt(1);
                insertDefinitions(cardId, card.getDefinitions());
                card.setId(cardId);
                card.setInsertTime(new Timestamp(new java.util.Date().getTime()));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting card entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
    }

    @Override
    public boolean generateCardsWithoutDefinitions(Set<Integer> wordIds, int dictionaryId, CardStatus status) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(TBL_CARD);
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

    private void insertDefinitions(int cardId, List<Definition> definitions) throws DaoException {
        for (Definition definition : definitions) {
            insertDefinition(cardId, definition);
        }
    }

    private void insertDefinition(int cardId, Definition definition) throws DaoException {
        String INSERT = "INSERT INTO "
                + TBL_DEFINITION + " ("
                + CARD_ID + ", "
                + GRAM_UNIT + ", "
                + DEFINITION
                + ") VALUES (?,?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setInt(1, cardId);
            statement.setString(2, definition.getGramUnit().name());
            statement.setString(3, definition.getValue());
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int definitionId = resultSet.getInt(1);
                insertMeanings(definitionId, definition.getMeanings());
                definition.setId(definitionId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting definition entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
    }

    private void insertMeanings(int definitionId, List<Meaning> meanings) throws DaoException {
        for (Meaning meaning : meanings) {
            insertMeaning(definitionId, meaning);
        }
    }

    private void insertMeaning(int definitionId, Meaning meaning) throws DaoException {
        String INSERT = "INSERT INTO "
                + TBL_MEANING + " ("
                + TRANSLATION + ", "
                + SYNONYM + ", "
                + ANTONYM + ", "
                + EXAMPLE + ", "
                + DEF_ID
                + ") VALUES (?,?,?,?,?)";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepareInsert(INSERT)) {
            statement.setString(1, meaning.getTranslation());
            statement.setString(2, meaning.getSynonym());
            statement.setString(3, meaning.getAntonym());
            statement.setString(4, meaning.getExample());
            statement.setInt(5, definitionId);
            statement.execute();
            // get last inserted id
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                int meaningId = resultSet.getInt(1);
                meaning.setId(meaningId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting meaning entity", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
    }

    @Override
    public void delete(Card card) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(TBL_CARD);
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
    public void update(Card card) throws DaoException {

        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        query.append(TBL_CARD);
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

        deleteFromDefinition(card);
        insertDefinitions(card.getId(), card.getDefinitions());
    }

    private void deleteFromDefinition(Card card) throws DaoException {
        String SQL = "DELETE FROM " + TBL_DEFINITION + " WHERE " + CARD_ID + "=?";
        try(PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting definition entity", ex);
        }
    }

    @Override
    public List<Card> selectCardsForDictionary(Dictionary dictionary) throws DaoException {
        String SELECT_ALL_CARDS_SQL = "SELECT * FROM " + TBL_CARD + " WHERE " + DIC_ID + "=?";
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
                card.setInsertTime(resultSet.getTimestamp(CREATE_TIME));
                card.setUpdateTime(resultSet.getTimestamp(UPDATE_TIME));
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

    @Override
    public List<Card> selectAllCardsSortedBy(String fieldName) throws DaoException {
        ResultSet resultSet = null;
        ArrayList<Card> data = new ArrayList<Card>();
        String SELECT_ALL_CARDS = "SELECT card.id, status, rating, create_time, update_time, word_id, dictionary_id " +
                "FROM card JOIN word ON card.word_id = word.id ORDER BY " + fieldName;
        try(PreparedStatement statement = prepare(SELECT_ALL_SQL)) {
            resultSet = statement.executeQuery(SELECT_ALL_CARDS);
            while (resultSet.next()) {
                Card card = new Card();
                card.setId(resultSet.getInt(ID));
                card.setStatus(CardStatus.valueOf(resultSet.getString(STATUS)));
                card.setRating(resultSet.getInt(RATING));
                card.setInsertTime(resultSet.getTimestamp(CREATE_TIME));
                card.setUpdateTime(resultSet.getTimestamp(UPDATE_TIME));
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

    @Override
    public int[] selectCardsForExercise(Dictionary dictionary, int limit) throws DaoException {

        int[] buffer = new int[limit]; // initial array with the largest possible capacity;
        int cnt = 0; // retrieved amount

        String SELECT_FOR_EXERCISE_SQL = "SELECT " + ID + " FROM " + TBL_CARD +
                " WHERE " + DIC_ID + "=? AND " + STATUS + "=?" + " ORDER BY " + CREATE_TIME + " LIMIT " + limit;
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SELECT_FOR_EXERCISE_SQL)) {
            statement.setInt(1, dictionary.getId());
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
        // downsize the array if retrieved amount is less then the limit
        if (cnt < limit) {
            int[] ids = new int[cnt];
            System.arraycopy(buffer, 0, ids, 0, cnt);
            return ids;
        }
        return buffer;
    }

    @Override
    public List<Definition> getDefinitionsFor(Card card) throws DaoException {
        ArrayList<Definition> definitions = new ArrayList<Definition>();
        String SQL = "SELECT * FROM " + TBL_DEFINITION + " WHERE " + CARD_ID + "=?";
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, card.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Definition definition = new Definition();
                definition.setId(resultSet.getInt(1));
                definition.setGramUnit(GramUnit.valueOf(resultSet.getString(2)));
                definition.setValue(resultSet.getString(3));
                definition.setCardId(resultSet.getInt(4));
                addMeanings(definition);
                definitions.add(definition);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving definition entities for card", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return definitions;
    }

    private void addMeanings(Definition definition) throws DaoException {
        ResultSet resultSet = null;
        String SQL = "SELECT * FROM " + TBL_MEANING + " WHERE " + DEF_ID + "=?";
        try (PreparedStatement statement = prepare(SQL)) {
            statement.setInt(1, definition.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Meaning meaning = new Meaning();
                meaning.setId(resultSet.getInt(1));
                meaning.setTranslation(resultSet.getString(2));
                meaning.setSynonym(resultSet.getString(3));
                meaning.setAntonym(resultSet.getString(4));
                meaning.setExample(resultSet.getString(5));
                meaning.setDefinitionId(resultSet.getInt(6));
                definition.add(meaning);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while selecting meaning entities for definition", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
    }

    @Override
    public IScore getScore(Dictionary dictionary) throws DaoException {
        Score score = new Score();
        String COUNT_SCORE_SQL = "SELECT " + STATUS + ", COUNT(" + STATUS + ") FROM " + TBL_CARD +
                " WHERE " + DIC_ID + "=? GROUP BY " + STATUS;
        ResultSet resultSet = null;
        try (PreparedStatement statement = prepare(COUNT_SCORE_SQL)) {
            statement.setInt(1, dictionary.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                CardStatus status = CardStatus.valueOf(resultSet.getString(STATUS));
                int count = resultSet.getInt(2);
                score.setScoreCount(status, count);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving score for dictionary", ex);
        } finally {
            CloseUtils.closeQuietly(resultSet);
        }
        return score;
    }

    @Override
    public void resetScore(Dictionary dictionary) throws DaoException {
        String RESET_SCORE_SQL = "UPDATE " + TBL_CARD + " SET " + RATING + "= ?, " + STATUS + "= ?" + " WHERE " + DIC_ID + "= ?";
        try (PreparedStatement statement = prepare(RESET_SCORE_SQL)) {
            statement.setInt(1, 0);
            statement.setString(2, CardStatus.DEFAULT_STATUS.name());
            statement.setInt(3, dictionary.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while resetting score for dictionary", ex);
        }
    }

}