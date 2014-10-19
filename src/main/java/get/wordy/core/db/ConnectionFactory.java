package get.wordy.core.db;

import get.wordy.core.ServerInfo;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.db.IConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionFactory implements IConnectionFactory {

    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());

    private static final ConnectionFactory INSTANCE = new ConnectionFactory();

    private Connection connection;
    private ServerInfo info;

    private ConnectionFactory() {
        // default
    }

    public static final ConnectionFactory getInstance() {
        return ConnectionFactory.INSTANCE;
    }

    @Override
    public void open() throws DaoException {
        String database = info.getDatabase();
        String host = info.getHost();
        String url = host + "/" + database + getParameters();
        try {
            close();
            connection = DriverManager.getConnection(url, info.getAccount());
            connection.setAutoCommit(false);
        } catch (SQLException ex) {
            throw new DaoException("Error while creating connection to MySQL database", ex);
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

    @Override
    public void commit() throws DaoException {
        try {
            connection.commit();
        } catch (SQLException ex) {
            throw new DaoException("Error while committing changes to MySQL database", ex);
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Error while rolling back connection to MySQL database", ex);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                CloseUtils.closeQuietly(connection);
            }
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Error while closing connection to database", ex);
        }
    }

    @Override
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