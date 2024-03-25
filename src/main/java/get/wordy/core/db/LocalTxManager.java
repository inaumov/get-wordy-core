package get.wordy.core.db;

import get.wordy.core.dao.exception.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class LocalTxManager {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTxManager.class);

    private final DataSource dataSource;

    private Connection connection;

    private LocalTxManager(DataSource dataSource) {
        // default
        this.dataSource = dataSource;
    }

    public static LocalTxManager withDataSource(DataSource dataSource) {
        return new LocalTxManager(dataSource);
    }

    public void open() throws DaoException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DataSourceUtils.getConnection(dataSource);
                connection.setAutoCommit(false);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while creating connection to database", ex);
        }
    }

    public void commit() throws DaoException {
        try {
            connection.commit();
        } catch (SQLException ex) {
            throw new DaoException("Error while committing changes to database", ex);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            LOG.warn("Error while rolling back changes from database", ex);
        }
    }

    public void close() {
        if (connection != null) {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Connection get() {
        if (connection == null) {
            throw new RuntimeException("Connection is not established right now");
        }
        return connection;
    }

}