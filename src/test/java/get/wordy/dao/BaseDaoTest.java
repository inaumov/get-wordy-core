package get.wordy.dao;

import get.wordy.core.db.ServerInfo;
import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.LocalTxManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public abstract class BaseDaoTest {

    protected LocalTxManager connection;
    protected static DaoFactory daoFactory;

    @BeforeEach
    public void setUp() throws Exception {
        // Set test server info
        if (connection != null && connection.get() != null && !connection.get().isClosed()) {
            return;
        }
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setUrl(System.getenv("get.wordy.jdbc.url"));
        serverInfo.getCredentials().setProperty("user", System.getenv("get.wordy.jdbc.user"));
        serverInfo.getCredentials().setProperty("password", System.getenv("get.wordy.jdbc.password"));
        daoFactory = DaoFactory.getFactory();

        connection = LocalTxManager.getInstance();
        connection.setServerInfo(serverInfo);
        connection.open();

        DatabaseMetaData metaData = connection.get().getMetaData();
        if (metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
            connection.get().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
    }

    @AfterEach
    public void tearDown() {
        connection.rollback();
    }

}