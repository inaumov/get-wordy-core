package get.wordy.dao.config;

import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.LocalTxManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
public class TestDaoConfig {

    @Bean
    public LocalTxManager txManager(DataSource dataSource) {
        return LocalTxManager.withDataSource(dataSource);
    }

    @Bean
    public DaoFactory daoFactory(LocalTxManager txManager) {
        return DaoFactory.withTxManager(txManager);
    }

}
