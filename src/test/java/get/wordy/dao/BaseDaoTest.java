package get.wordy.dao;

import get.wordy.core.ServerInfo;
import get.wordy.core.bean.Collocation;
import get.wordy.core.bean.Context;
import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.ConnectionWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public abstract class BaseDaoTest {

    protected ConnectionWrapper connect;
    protected static DaoFactory factory;

    @BeforeEach
    public void setUp() throws Exception {
        // Set test server info
        if (connect != null && connect.get() != null && !connect.get().isClosed()) {
            return;
        }
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setHost(System.getenv("get.wordy.jdbc.url"));
        serverInfo.setDatabase(System.getenv("get.wordy.jdbc.database"));
        serverInfo.getAccount().setProperty("user", System.getenv("get.wordy.jdbc.user"));
        serverInfo.getAccount().setProperty("password", System.getenv("get.wordy.jdbc.password"));
        factory = DaoFactory.getFactory();

        connect = ConnectionWrapper.getInstance();
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

    Context prepareContext(int wordId, int contextId) {
        String d = "example" + contextId;
        return new Context(contextId, d, wordId);
    }

    Collocation prepareCollocation(int wordId, int collocationId) {
        String e = "example" + collocationId;
        return new Collocation(collocationId, e, wordId);
    }

}