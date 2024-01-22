package get.wordy.core.db;

import get.wordy.core.ServerInfo;
import get.wordy.core.dao.exception.DaoException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionWrapper {

    private static final Logger LOG = Logger.getLogger(ConnectionWrapper.class.getName());

    private static final ConnectionWrapper INSTANCE = new ConnectionWrapper();

    private Connection connection;
    private ServerInfo info;

    private ConnectionWrapper() {
        // default
    }

    public static ConnectionWrapper getInstance() {
        return ConnectionWrapper.INSTANCE;
    }

    public void open() throws DaoException {
        String database = info.getDatabase();
        String host = info.getHost();
        String url = host + "/" + database + getParameters();
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, info.getAccount());
                connection.setAutoCommit(false);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error while creating connection to database", ex);
        }
    }

    private String getParameters() {
        Set<String> parameters = info.getParameters();
        if (parameters.isEmpty()) {
            return "";
        }
        String str = "?";
        Iterator<String> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            str = str.concat(iterator.next());
            if (iterator.hasNext()) {
                str = str.concat("&");
            }
        }
        return str;
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
            LOG.log(Level.WARNING, "Error while rolling back changes from database", ex);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                CloseUtils.closeQuietly(connection);
            }
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Error while closing connection to database", ex);
        }
    }

    public Connection get() {
        if (connection == null) {
            throw new NullPointerException("Connection is not opened right now");
        }
        return connection;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.info = serverInfo;
    }

}