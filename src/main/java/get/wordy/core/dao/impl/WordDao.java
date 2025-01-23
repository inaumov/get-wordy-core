package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.InContext;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Word;
import get.wordy.core.db.LocalTxManager;

import java.sql.*;
import java.util.*;

public class WordDao extends BaseDao<Word> {

    public static final String INSERT_QUERY = "INSERT INTO words (word, part_of_speech, transcription, meaning) VALUES (?, ?, ?, ?)";
    public static final String DELETE_QUERY = "DELETE FROM words WHERE id = ?";
    public static final String UPDATE_QUERY = "UPDATE words SET word = ?, part_of_speech = ?, transcription = ?, meaning = ? WHERE id = ?";
    public static final String SELECT_ALL_QUERY = "SELECT * FROM words WHERE id IN (%s)";
    public static final String SELECT_BY_ID_QUERY = "SELECT * FROM words WHERE id = ?";

    // sentences and collocations
    private static final String INSERT_SENTENCE_QUERY = "INSERT INTO in_context (word_id, example, matched_words) VALUES (?,?,?)";
    private static final String INSERT_COLLOCATIONS_QUERY = "INSERT INTO collocations (word_id, example) VALUES (?,?)";
    private static final String SELECT_FROM_CONTEXT_QUERY = "SELECT * FROM in_context WHERE word_id=?";
    private static final String SELECT_COLLOCATIONS_QUERY = "SELECT * FROM collocations WHERE word_id=?";
    private static final String DELETE_FROM_CONTEXT_QUERY = "DELETE FROM in_context WHERE word_id=?";
    private static final String DELETE_COLLOCATIONS_QUERY = "DELETE FROM collocations WHERE word_id=?";

    WordDao(LocalTxManager txManager) {
        super(txManager);
    }

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
                insertSentences(id, word.getSentences());
                insertCollocations(id, word.getCollocations());
                return word.withId(id);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting a word record", ex);
        }
        return word;
    }

    public List<Word> addWords(Collection<Word> words) throws DaoException {

        // copy to collect sentences and collocations for bulk insertion later
        List<Word> copyWithIds = new ArrayList<>();

        try (var statement = prepareStatementForInsert(INSERT_QUERY)) {
            for (Word word : words) {
                statement.setString(1, word.getValue());
                statement.setString(2, word.getPartOfSpeech());
                statement.setString(3, word.getTranscription());
                statement.setString(4, word.getMeaning());
                statement.execute();
                // get last inserted id
                ResultSet keys = statement.getGeneratedKeys();
                while (keys.next()) {
                    int id = keys.getInt(1);
                    copyWithIds.add(word.withId(id));
                }
            }

            insertAllSentencesInBatch(copyWithIds);
            insertAllCollocationsInBatch(copyWithIds);

        } catch (SQLException ex) {
            throw new DaoException("Error while generating word records", ex);
        }

        return copyWithIds;
    }

    public void delete(int wordId) throws DaoException {
        try (var statement = prepareStatement(DELETE_QUERY)) {
            statement.setInt(1, wordId);
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting a word record", ex);
        }
    }

    public int update(Word word) throws DaoException {
        int records = 0;
        try (var statement = prepareStatement(UPDATE_QUERY)) {
            statement.setString(1, word.getValue());
            statement.setString(2, word.getPartOfSpeech());
            statement.setString(3, word.getTranscription());
            statement.setString(4, word.getMeaning());
            statement.setInt(5, word.getId());
            records = statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating a word record", ex);
        }

        deleteFromContext(word.getId());
        deleteFromCollocations(word.getId());

        insertSentences(word.getId(), word.getSentences());
        insertCollocations(word.getId(), word.getCollocations());

        return records;
    }

    public List<Word> selectAll(Set<Integer> wordsRefs) throws DaoException {

        if (wordsRefs == null || wordsRefs.isEmpty()) {
            throw new IllegalArgumentException("The set of wordsRefs cannot be null or empty");
        }

        // generate the dynamic query
        String placeholders = String.join(",", Collections.nCopies(wordsRefs.size(), "?"));
        String sql = String.format(SELECT_ALL_QUERY, placeholders);

        List<Word> words = new ArrayList<>();

        try (var preparedStatement = prepareStatement(sql)) {
            // bind parameters
            int index = 1;
            for (Integer id : wordsRefs) {
                preparedStatement.setInt(index++, id);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                var word = mapResultSetToWordEntity(resultSet);
                word.setSentences(getSentencesFor(word.getId()));
                word.setCollocations(getCollocationsFor(word.getId()));
                words.add(word);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving word records by ids", ex);
        }
        return words;
    }

    public Word selectById(int wordId) throws DaoException {
        try (var statement = prepareStatement(SELECT_BY_ID_QUERY)) {
            statement.setInt(1, wordId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Word word = mapResultSetToWordEntity(resultSet);
                word.setSentences(getSentencesFor(wordId));
                word.setCollocations(getCollocationsFor(wordId));
                return word;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving a word record", ex);
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

    public List<InContext> getSentencesFor(int wordId) throws DaoException {
        ArrayList<InContext> result = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_FROM_CONTEXT_QUERY)) {
            statement.setInt(1, wordId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String example = resultSet.getString("example");
                InContext sentence = new InContext(example);
                result.add(sentence);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving sentence examples for word with id = " + wordId, ex);
        }
        return result;
    }

    public List<String> getCollocationsFor(int wordId) throws DaoException {
        ArrayList<String> collocations = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_COLLOCATIONS_QUERY)) {
            statement.setInt(1, wordId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String example = resultSet.getString("example");
                collocations.add(example);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving collocations for word with id = " + wordId, ex);
        }
        return collocations;
    }

    private void insertSentences(int wordId, List<InContext> sentences) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_SENTENCE_QUERY)) {
            for (InContext sentence : sentences) {
                statement.setInt(1, wordId);
                statement.setString(2, sentence.getExample());
                statement.setString(3, sentence.getMatchedWords());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting sentence examples", ex);
        }
    }

    public void insertAllSentencesInBatch(List<Word> words) throws DaoException {
        if (words.isEmpty()) {
            return;
        }
        try (var statement = prepareStatementForInsert(INSERT_SENTENCE_QUERY)) {
            for (Word word : words) {
                for (InContext sentence : word.getSentences()) {
                    statement.setInt(1, word.getId());
                    statement.setString(2, sentence.getExample());
                    statement.setString(3, sentence.getMatchedWords());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting sentence examples", ex);
        }
    }

    private void insertCollocations(int wordId, List<String> collocations) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_COLLOCATIONS_QUERY)) {
            for (String collocation : collocations) {
                statement.setInt(1, wordId);
                statement.setString(2, collocation);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting collocation examples", ex);
        }
    }

    public void insertAllCollocationsInBatch(List<Word> words) throws DaoException {
        if (words.isEmpty()) {
            return;
        }
        try (var statement = prepareStatementForInsert(INSERT_COLLOCATIONS_QUERY)) {
            for (Word word : words) {
                for (String collocation : word.getCollocations()) {
                    statement.setInt(1, word.getId());
                    statement.setString(2, collocation);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting collocation examples", ex);
        }
    }


    private void deleteFromContext(int wordId) throws DaoException {
        try (var statement = prepareStatement(DELETE_FROM_CONTEXT_QUERY)) {
            statement.setInt(1, wordId);
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting in_context record", ex);
        }
    }

    private void deleteFromCollocations(int wordId) throws DaoException {
        try (var statement = prepareStatement(DELETE_COLLOCATIONS_QUERY)) {
            statement.setInt(1, wordId);
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting collocation record", ex);
        }
    }

}