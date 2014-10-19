package get.wordy.core.dao.impl;

import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.IDictionaryDao;
import get.wordy.core.api.db.IConnectionFactory;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.db.CloseUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DictionaryDao extends BaseDao implements IDictionaryDao {

    public static final String INSERT_SQL = "INSERT INTO dictionary (name) VALUES (?)";
    public static final String DELETE_SQL = "DELETE FROM dictionary WHERE id = ?";
    public static final String UPDATE_SQL = "UPDATE dictionary SET name = ? WHERE id = ?";
    public static final String SELECT_ALL_SQL = "SELECT * FROM dictionary ORDER BY name";
    public static final String COUNT_SQL = "SELECT COUNT(name) FROM dictionary";

    DictionaryDao(IConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void insert(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepareInsert(INSERT_SQL)) {
            statement.setString(1, dictionary.getName());
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
    }

    @Override
    public void delete(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepareInsert(DELETE_SQL)) {
            statement.setInt(1, dictionary.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting dictionary entity", ex);
        }
    }

    @Override
    public void update(Dictionary dictionary) throws DaoException {
        try (PreparedStatement statement = prepareInsert(UPDATE_SQL)) {
            statement.setString(1, dictionary.getName());
            statement.setInt(2, dictionary.getId());
            statement.execute();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting dictionary entity", ex);
        }
    }

    @Override
    public List<Dictionary> selectAll() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        ArrayList<Dictionary> dictionaries = new ArrayList<Dictionary>();
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_SQL);
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String dictionary = resultSet.getString(2);
                dictionaries.add(new Dictionary(id, dictionary));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all dictionary entities", ex);
        } finally {
            CloseUtils.closeQuietly(statement);
        }
        return dictionaries;
    }

    @Override
    public int count() throws DaoException {
        Connection connection = getConnection();
        Statement statement = null;
        int count = -1;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(COUNT_SQL);
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

}