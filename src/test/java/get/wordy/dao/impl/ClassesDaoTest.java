package get.wordy.dao.impl;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.impl.ClassesDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {ClassesDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:classes.sql") // load predefined inserts
public class ClassesDaoTest {

    @Autowired
    private ClassesDao classesDao;

    @Test
    public void testSelectAllByOwnerId() {
        OwnerId ownerId = new OwnerId("owner123", "user");

        // fetch data and verify
        List<ClassInfo> results = classesDao.selectAllByOwnerId(ownerId);
        assertEquals(2, results.size());
        assertEquals("Math 101", results.get(0).getName());
        assertEquals("Science 101", results.get(1).getName());
    }

    @Test
    public void testUpdate() {
        OwnerId ownerId = new OwnerId("owner123", "user");
        ClassInfo updatedDetails = new ClassInfo("class001", "Math 102", "offline", "intermediate", "updated.pdf", "Advanced topics");

        // update and verify
        classesDao.update(ownerId, updatedDetails);
        List<ClassInfo> results = classesDao.selectAllByOwnerId(ownerId);

        assertEquals(2, results.size());
        ClassInfo updatedClass = results.stream()
                .filter(c -> c.getClassId().equals("class001"))
                .findFirst()
                .orElseThrow();
        assertEquals("Math 102", updatedClass.getName());
        assertEquals("offline", updatedClass.getFormat());
    }

    @Test
    public void testDelete() {
        OwnerId ownerId = new OwnerId("owner123", "user");

        // delete and verify
        classesDao.delete(ownerId, "class001");
        List<ClassInfo> results = classesDao.selectAllByOwnerId(ownerId);

        assertEquals(1, results.size());
        assertFalse(results
                .stream()
                .anyMatch(c -> c.getClassId().equals("class001"))
        );
    }
}
