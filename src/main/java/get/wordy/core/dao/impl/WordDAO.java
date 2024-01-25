package get.wordy.core.dao.impl;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.bean.Word;
import get.wordy.core.db.CloseUtils;
import get.wordy.core.db.ConnectionWrapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordDao extends BaseDao<Word> {

    public static final String INSERT_SQL = "INSERT INTO words (word, part_of_speech, transcription, meaning) VALUES (?, ?, ?, ?)";
    public static final String DELETE_SQL = "DELETE FROM words WHERE id = ?";
    public static final String UPDATE_SQL = "UPDATE words SET word = ?, part_of_speech = ?, transcription = ?, meaning = ? WHERE id = ?";
    public static final String SELECT_ALL_SQL = "SELECT * FROM words ORDER BY id";

    WordDao(ConnectionWrapper connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Word insert(Word word) throws DaoException {
        try (PreparedStatement statement = prepareInsert(INSERT_SQL)) {
            statement.setString(1, word.getValue());
            statement.setString(2, word.getPartOfSpeech());
            statement.setString(3, word.getTranscription());
            statement.setString(4, word.getMeaning());
            statement.execute();
            // get last inserted id
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                return word.withId(id);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting word entity", ex);
        }
        return word;
    }

    public Set<Integer> generate(Set<String> words) throws DaoException {
        try (PreparedStatement statement = prepareInsert("INSERT INTO words (word) VALUES (?)")) {
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
    public Word update(Word word) throws DaoException {
        try (PreparedStatement statement = prepare(UPDATE_SQL)) {
            statement.setString(1, word.getValue());
            statement.setString(2, word.getPartOfSpeech());
            statement.setString(3, word.getTranscription());
            statement.setString(4, word.getMeaning());
            statement.setInt(5, word.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating word entity", ex);
        }
        return word;
    }

    public List<Word> selectAll() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        List<Word> words = new ArrayList<>();
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_SQL);
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String word = resultSet.getString(2);
                String partOfSpeech = resultSet.getString(3);
                String transcription = resultSet.getString(4);
                String meaning = resultSet.getString(5);
                words.add(new Word(id, word, partOfSpeech, transcription, meaning));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all word entities", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return words;
    }

}