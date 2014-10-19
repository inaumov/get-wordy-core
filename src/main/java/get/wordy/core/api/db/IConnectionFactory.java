package get.wordy.core.api.db;

import get.wordy.core.api.dao.DaoException;

import java.sql.Connection;

public interface IConnectionFactory {

    void open() throws DaoException;

    void commit() throws DaoException;

    void rollback();

    void close();

    Connection get();

}