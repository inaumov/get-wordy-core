package get.wordy.core.dao.impl;

import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.db.LocalTxManager;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DictionaryDao extends BaseDao<Dictionary> {

    public static final String INSERT_QUERY = "INSERT INTO dictionaries (name, picture_url) VALUES (?, ?)";
    public static final String DELETE_QUERY = "DELETE FROM dictionaries WHERE id = ?";
    public static final String UPDATE_NAME_QUERY = "UPDATE dictionaries SET name = ? WHERE id = ?";
    public static final String UPDATE_PIC_QUERY = "UPDATE dictionaries SET picture_url = ? WHERE id = ?";
    public static final String SELECT_ALL_WITH_CARDS_TOTAL_QUERY = """
            SELECT d.*, count(c.id) cards_total FROM dictionaries d LEFT OUTER JOIN cards c ON d.id = c.dictionary_id WHERE d.owner_id = ? AND d.owner_type = ? GROUP BY d.id, d.name ORDER BY d.name;
            """;
    public static final String SELECT_BY_ID_WITH_CARDS_TOTAL_QUERY = """
            SELECT d.*, count(c.id) cards_total FROM dictionaries d LEFT OUTER JOIN cards c ON d.id = c.dictionary_id WHERE d.id = ? GROUP BY d.id, d.name ORDER BY d.name;
            """;
    public static final String COUNT_QUERY = "SELECT COUNT(d.name) FROM dictionaries d";

    DictionaryDao(LocalTxManager txManager) {
        super(txManager);
    }

    public Dictionary insert(Dictionary dictionary) throws DaoException {
        try (var statement = prepareStatementForInsert(INSERT_QUERY)) {
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
            throw new DaoException("Error while inserting a dictionary record", ex);
        }
        return dictionary;
    }

    @Override
    public void delete(int dictionaryId) throws DaoException {
        try (var statement = prepareStatement(DELETE_QUERY)) {
            statement.setInt(1, dictionaryId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while deleting a dictionary record", ex);
        }
    }

    @Override
    public Dictionary update(Dictionary dictionary) throws DaoException {
        String query;
        String paramValue;
        if (StringUtils.hasText(dictionary.getName())) {
            query = UPDATE_NAME_QUERY;
            paramValue = dictionary.getName();
        } else {
            query = UPDATE_PIC_QUERY;
            paramValue = dictionary.getPicture();
        }
        try (var statement = prepareStatement(query)) {
            statement.setString(1, paramValue);
            statement.setInt(2, dictionary.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating a dictionary record", ex);
        }
        return dictionary;
    }

    public List<Dictionary> selectAllByOwnerId(OwnerId ownerId) throws DaoException {
        ArrayList<Dictionary> dictionaries = new ArrayList<>();
        try (var statement = prepareStatement(SELECT_ALL_WITH_CARDS_TOTAL_QUERY)) {
            statement.setString(1, ownerId.ownerId());
            statement.setString(2, ownerId.ownerType());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                String picture = resultSet.getString(3);
                int cardsTotal = resultSet.getInt("cards_total");
                dictionaries.add(new Dictionary(id, name, picture, cardsTotal));
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while retrieving all dictionary records", ex);
        }
        return dictionaries;
    }

    public int count() throws DaoException {
        int count = -1;
        try (var statement = getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(COUNT_QUERY);
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while counting all dictionary records", ex);
        }
        return count;
    }

    public Dictionary selectById(int dictionaryId) throws DaoException {
        try (var statement = prepareStatement(SELECT_BY_ID_WITH_CARDS_TOTAL_QUERY)) {
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
            throw new DaoException("Error while retrieving a dictionary record", ex);
        }
        return null;
    }

    public void updateOwnerRelation(OwnerId user, Dictionary dictionary) throws DaoException {
        try (var statement = prepareStatement("update dictionaries set owner_id = ?, owner_type = ? where id = ?")) {
            statement.setString(1, user.ownerId());
            statement.setString(2, user.ownerType());
            statement.setInt(3, dictionary.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Error while updating an owner relation to dictionary record", ex);
        }
    }

}