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