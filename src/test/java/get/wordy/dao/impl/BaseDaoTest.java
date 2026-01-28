package get.wordy.dao.impl;

import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate testJdbcTemplate;

    @BeforeEach
    void setup() {
        testJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        testJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch");
    }

}