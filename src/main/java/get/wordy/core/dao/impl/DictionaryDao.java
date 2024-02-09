package get.wordy.core.dao.impl;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.db.CloseUtils;
import get.wordy.core.db.ConnectionWrapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DictionaryDao extends BaseDao<Dictionary> {

    public static final String INSERT_QUERY = "INSERT INTO dictionaries (name, picture_url) VALUES (?, ?)";
    public static final String DELETE_QUERY = "DELETE FROM dictionaries WHERE id = ?";
    public static final String UPDATE_NAME_QUERY = "UPDATE dictionaries SET name = ? WHERE id = ?";
    public static final String UPDATE_PIC_QUERY = "UPDATE dictionaries SET picture_url = ? WHERE id = ?";
    public static final String SELECT_ALL_QUERY = "SELECT * FROM dictionaries ORDER BY name";
    public static final String SELECT_ALL_WITH_CARDS_TOTAL_QUERY = """
            SELECT d.*, count(c.id) cards_total FROM dictionaries d LEFT OUTER JOIN cards c ON d.id = c.dictionary_id GROUP BY d.id ORDER BY d.name;
            """;
    public static final String SELECT_BY_ID_WITH_CARDS_TOTAL_QUERY = """
            SELECT d.*, count(c.id) cards_total FROM dictionaries d LEFT OUTER JOIN cards c ON d.id = c.dictionary_id  WHERE d.id = ? GROUP BY d.id ORDER BY d.name;
            """;
    public static final String COUNT_QUERY = "SELECT COUNT(name) FROM dictionaries";

    DictionaryDao(ConnectionWrapper connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Dictionary insert(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepareInsert(INSERT_QUERY)) {
            statement.setString(1, dictionary.getName());
            statement.setString(2, dictionary.getPicture());
            statement.execute();
            // get last inserted id
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                int dictionaryId = keys.getInt(1);
                dictionary.setId(dictionaryId);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while inserting dictionary entity", ex);
        }
        return dictionary;
    }

    @Override
    public void delete(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepareInsert(DELETE_QUERY)) {
            statement.setInt(1, dictionary.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting dictionary entity", ex);
        }
    }

    @Override
    public Dictionary update(Dictionary dictionary) throws DaoException {
        String query = null;
        String paramValue = null;
        if (dictionary.getName() != null) {
            query = UPDATE_NAME_QUERY;
            paramValue = dictionary.getName();
        } else if (dictionary.getPicture() != null) {
            query = UPDATE_PIC_QUERY;
            paramValue = dictionary.getPicture();
        }
        try (PreparedStatement statement = prepareInsert(query)) {
            statement.setString(1, paramValue);
            statement.setInt(2, dictionary.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating dictionary entity", ex);
        }
        return dictionary;
    }

    public List<Dictionary> selectAll() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        ArrayList<Dictionary> dictionaries = new ArrayList<>();
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_WITH_CARDS_TOTAL_QUERY);
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                String picture = resultSet.getString(3);
                int cardsTotal = resultSet.getInt("cards_total");
                dictionaries.add(new Dictionary(id, name, picture, cardsTotal));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all dictionary entities", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return dictionaries;
    }

    public int count() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        int count = -1;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(COUNT_QUERY);
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while counting all dictionary entities", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return count;
    }

    public Dictionary getDictionary(int dictionaryId) throws DaoException {
        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(SELECT_BY_ID_WITH_CARDS_TOTAL_QUERY);
            statement.setInt(1, dictionaryId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                String picture = resultSet.getString(3);
                int cardsTotal = resultSet.getInt("cards_total");
                return new Dictionary(id, name, picture, cardsTotal);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving a dictionary by id", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return null;
    }

}