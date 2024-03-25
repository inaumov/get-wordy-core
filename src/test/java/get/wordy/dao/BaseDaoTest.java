package get.wordy.dao;

import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.LocalTxManager;
import get.wordy.dao.config.SpringJdbcConfig;
import get.wordy.dao.config.TestDaoConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@Sql(
        value = "/test-data.sql",
        executionPhase = BEFORE_TEST_CLASS,
        config = @SqlConfig(encoding = "utf-8")
)
@ContextConfiguration(classes = {
        SpringJdbcConfig.class,
        TestDaoConfig.class
})
@ExtendWith(SpringExtension.class)
public abstract class BaseDaoTest {

    @Autowired
    private LocalTxManager txManager;
    @Autowired
    protected DaoFactory daoFactory;

    @BeforeEach
    public void setUp() throws Exception {
        txManager.open();

        DatabaseMetaData metaData = txManager.get().getMetaData();
        if (metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
            txManager.get().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
    }

    @AfterEach
    public void tearDown() {
        txManager.rollback();
    }

}