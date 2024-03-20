package get.wordy.dao;

import get.wordy.core.ServerInfo;
import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.LocalTxManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public abstract class BaseDaoTest {

    protected LocalTxManager connect;
    protected static DaoFactory daoFactory;

    @BeforeEach
    public void setUp() throws Exception {
        // Set test server info
        if (connect != null && connect.get() != null && !connect.get().isClosed()) {
            return;
        }
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setUrl(System.getenv("get.wordy.jdbc.url"));
        serverInfo.getCredentials().setProperty("user", System.getenv("get.wordy.jdbc.user"));
        serverInfo.getCredentials().setProperty("password", System.getenv("get.wordy.jdbc.password"));
        daoFactory = DaoFactory.getFactory();

        connect = LocalTxManager.getInstance();
        connect.setServerInfo(serverInfo);
        connect.open();

        DatabaseMetaData metaData = connect.get().getMetaData();
        if (metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
            connect.get().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
    }

    @AfterEach
    public void tearDown() {
        connect.rollback();
    }

}