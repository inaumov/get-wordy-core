package get.wordy.dao.impl;

import get.wordy.core.api.bean.wrapper.VocabularySummary;
import get.wordy.core.api.id.OwnersId;
import get.wordy.core.dao.impl.VocabularyDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Rollback
@SpringJUnitConfig(classes = {VocabularyDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/vocab-summary.sql") // load predefined inserts
class VocabSummaryDaoTest extends BaseDaoTest {

    @Autowired
    private VocabularyDao vocabularyDao;

    @Test
    void shouldReturnSummariesForClasses() {
        OwnersId ownersId = new OwnersId(Set.of("class-1", "class-2", "class-3"), "class");

        List<VocabularySummary> result = vocabularyDao.findVocabularySummariesByType(ownersId);

        assertThat(result).hasSize(3);

        // Expected Summary:
        // class-1 → 2 not shared (last interaction: 1 day ago)
        // class-2 → 2 not shared (last interaction: 1 hour ago)
        // class-3 → 0 not shared (last interaction: 5 days ago)

        assertThat(result).anySatisfy(summary -> {
            assertThat(summary.ownerId()).isEqualTo("class-1");
            assertThat(summary.notSharedCount()).isEqualTo(2);
            assertThat(summary.lastUpdatedAt()).isNotNull();
        });

        assertThat(result).anySatisfy(summary -> {
            assertThat(summary.ownerId()).isEqualTo("class-2");
            assertThat(summary.notSharedCount()).isEqualTo(2);
            assertThat(summary.lastUpdatedAt()).isNotNull();
        });

        assertThat(result).anySatisfy(summary -> {
            assertThat(summary.ownerId()).isEqualTo("class-3");
            assertThat(summary.notSharedCount()).isEqualTo(0);
            assertThat(summary.lastUpdatedAt()).isNotNull();
        });
    }

}
