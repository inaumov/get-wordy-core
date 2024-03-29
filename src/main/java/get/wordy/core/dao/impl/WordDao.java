package get.wordy.core.dao.impl;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Word;
import get.wordy.core.db.LocalTxManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordDao extends BaseDao<Word> {

    public static final String INSERT_QUERY = "INSERT INTO words (word, part_of_speech, transcription, meaning) VALUES (?, ?, ?, ?)";
    public static final String DELETE_QUERY = "DELETE FROM words WHERE id = ?";
    public static final String UPDATE_QUERY = "UPDATE words SET word = ?, part_of_speech = ?, transcription = ?, meaning = ? WHERE id = ?";
    public static final String SELECT_ALL_QUERY = "SELECT * FROM words ORDER BY id";
    public static final String SELECT_BY_ID_QUERY = "SELECT * FROM words WHERE id = ?";
    public static final String INSERT_WORD_BATCH_QUERY = "INSERT INTO words (word) VALUES (?)";

    WordDao(LocalTxManager txManager) {
        super(txManager);
    }

    @Override
    public Word insert(Word word) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_QUERY)) {
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
        try (var statement = prepareStatementForInsert(INSERT_WORD_BATCH_QUERY)) {
            for (String word : words) {
                statement.setString(1, word);
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
            throw new DaoException("Error while generating word entity", ex);
        }
    }

    @Override
    public void delete(Word word) throws DaoException {
        try (var statement = prepareStatement(DELETE_QUERY)) {
            statement.setInt(1, word.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting word entity", ex);
        }
    }

    @Override
    public Word update(Word word) throws DaoException {
        try (var statement = prepareStatement(UPDATE_QUERY)) {
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
        List<Word> words = new ArrayList<>();
        try (var statement = getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_QUERY);
            while (resultSet.next()) {
                var word = mapResultSetToWordEntity(resultSet);
                words.add(word);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all word entities", ex);
        }
        return words;
    }

    public Word selectById(int wordId) throws DaoException {
        try (var statement = prepareStatement(SELECT_BY_ID_QUERY)) {
            statement.setInt(1, wordId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapResultSetToWordEntity(resultSet);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving a word by id", ex);
        }
        return null;
    }

    private Word mapResultSetToWordEntity(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(1);
        String word = resultSet.getString(2);
        String partOfSpeech = resultSet.getString(3);
        String transcription = resultSet.getString(4);
        String meaning = resultSet.getString(5);
        return new Word(id, word, partOfSpeech, transcription, meaning);
    }

}