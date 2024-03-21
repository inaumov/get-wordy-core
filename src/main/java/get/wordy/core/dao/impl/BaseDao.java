package get.wordy.core.dao.impl;

import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.db.LocalTxManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseDao<T> {

    private final LocalTxManager txManager;

    BaseDao(LocalTxManager txManager) {
        this.txManager = txManager;
    }

    Connection getConnection() {
        return txManager.get();
    }

    PreparedStatement prepareStatement(String query) throws SQLException {
        return getConnection().prepareStatement(query);
    }

    PreparedStatement prepareStatementForInsert(String query) throws SQLException {
        return getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    }

    // crud

    public abstract T insert(T record) throws DaoException;

    public abstract T selectById(int id) throws DaoException;

    public abstract T update(T record) throws DaoException;

    public abstract void delete(T record) throws DaoException;

}