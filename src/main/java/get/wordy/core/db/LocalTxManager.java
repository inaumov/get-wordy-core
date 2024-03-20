package get.wordy.core.db;

import get.wordy.core.dao.exception.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LocalTxManager {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTxManager.class);

    private static final LocalTxManager INSTANCE = new LocalTxManager();

    private Connection connection;
    private ServerInfo info;

    private LocalTxManager() {
        // default
    }

    public static LocalTxManager getInstance() {
        return LocalTxManager.INSTANCE;
    }

    public void open() throws DaoException {
        String url = info.getUrl();
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, info.getCredentials());
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
        try {
            if (connection != null && !connection.isClosed()) {
                CloseUtils.closeQuietly(connection);
            }
        } catch (SQLException ex) {
            LOG.warn("Error while closing connection to database", ex);
        }
    }

    public Connection get() {
        if (connection == null) {
            throw new NullPointerException("Connection is not established right now");
        }
        return connection;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.info = serverInfo;
    }

}