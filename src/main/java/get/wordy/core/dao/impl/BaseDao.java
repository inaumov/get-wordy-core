package get.wordy.core.dao.impl;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.ConnectionWrapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseDao<T> {

    private final ConnectionWrapper connectionFactory;

    BaseDao(ConnectionWrapper connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    Connection getConnection() {
        return connectionFactory.get();
    }

    PreparedStatement prepare(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }

    PreparedStatement prepareInsert(String sql) throws SQLException {
        return getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    public abstract T insert(T record) throws DaoException;

    public abstract T update(T record) throws DaoException;

    public abstract void delete(T record) throws DaoException;

}