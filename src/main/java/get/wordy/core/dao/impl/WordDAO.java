package get.wordy.core.dao.impl;

import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.IWordDao;
import get.wordy.core.api.db.IConnectionFactory;
import get.wordy.core.bean.Word;
import get.wordy.core.db.CloseUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordDao extends BaseDao implements IWordDao {

    public static final String INSERT_SQL = "INSERT INTO word (value, transcription) VALUES (?, ?)";
    public static final String DELETE_SQL = "DELETE FROM word WHERE word.id = ?";
    public static final String UPDATE_SQL = "UPDATE word SET value = ?, transcription = ? WHERE id = ?";
    public static final String SELECT_ALL_SQL = "SELECT * FROM word ORDER BY id";

    WordDao(IConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void insert(Word word) throws DaoException {
        try (PreparedStatement statement = prepareInsert(INSERT_SQL)) {
            statement.setString(1, word.getValue());
            statement.setString(2, word.getTranscription());
            statement.execute();
            // get last inserted id
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                word.setId(id);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting word entity", ex);
        }
    }

    @Override
    public Set<Integer> generate(Set<String> words) throws DaoException {
        try (PreparedStatement statement = prepareInsert("INSERT INTO word (value) VALUES (?)")) {
            for (String word : words) {
                statement.setString(1, word);
                statement.addBatch();
            }
            statement.executeBatch();
            // get last inserted id
            ResultSet keys = statement.getGeneratedKeys();
            Set<Integer> ids = new HashSet<>();
            if (keys.next()) {
                ids.add(keys.getInt(1));
            }
            return ids;
        } catch (SQLException ex) {
            throw new DaoException("Error while generating word entity", ex);
        }
    }

    @Override
    public void delete(Word word) throws DaoException {
        try (PreparedStatement statement = prepare(DELETE_SQL)) {
            statement.setInt(1, word.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting word entity", ex);
        }
    }

    @Override
    public void update(Word word) throws DaoException {
        try (PreparedStatement statement = prepare(UPDATE_SQL)) {
            statement.setString(1, word.getValue());
            statement.setString(2, word.getTranscription());
            statement.setInt(3, word.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating word entity", ex);
        }
    }

    @Override
    public List<Word> selectAll() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        List<Word> words = new ArrayList<Word>();
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_SQL);
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String word = resultSet.getString(2);
                String transcription = resultSet.getString(3);
                words.add(new Word(id, word, transcription));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all word entities", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return words;
    }

}