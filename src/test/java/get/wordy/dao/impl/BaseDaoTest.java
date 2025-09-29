package get.wordy.dao.impl;

import get.wordy.dao.config.SpringJdbcConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@Sql(
        value = "/test-data.sql",
        executionPhase = BEFORE_TEST_CLASS,
        config = @SqlConfig(encoding = "utf-8")
)
@ContextConfiguration(classes = {
        SpringJdbcConfig.class
})
public abstract class BaseDaoTest {

}