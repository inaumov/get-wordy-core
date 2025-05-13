package get.wordy.dao.impl;

import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.NotFoundException;
import get.wordy.core.dao.impl.ClassAccessDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {ClassAccessDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:classes.sql")
@Rollback
public class ClassAccessDaoTest {

    @Autowired
    private ClassAccessDao classAccessDao;

    @Test
    public void testGrantAndCheckAccess() {
        String classId = "class1";
        String userId = "user123";

        classAccessDao.grantAccess(classId, userId);
        assertTrue(classAccessDao.hasViewAccess(classId, userId));

        classAccessDao.revokeAccess(classId, userId);
        assertFalse(classAccessDao.hasViewAccess(classId, userId));
    }

    @Test
    void testFindAccessibleClasses() {
        // given
        String viewerId = "viewer123";

        // when
        Map<String, Boolean> accessibleClasses = classAccessDao.findAccessibleClasses(viewerId);

        // then
        assertNotNull(accessibleClasses);
        assertEquals(2, accessibleClasses.size()); // viewer123 has access to 2 classes

        Set<String> expectedClassIds = Set.of("class1", "class2");
        accessibleClasses.keySet().forEach(classId ->
                assertTrue(expectedClassIds.contains(classId))
        );
    }

    @Test
    void hasAdminAccess() {
        boolean adminCanManage = classAccessDao.hasFullAccess("class1", new OwnerId("user123", "user"));
        assertTrue(adminCanManage);
    }

    @Test
    void hasViewerAccess() {
        String viewerId = "viewer123";
        boolean userCanSee = classAccessDao.hasViewAccess("class1", viewerId);
        assertTrue(userCanSee);

        boolean userCannotManage = classAccessDao.hasFullAccess("class1", new OwnerId(viewerId, "user"));
        assertFalse(userCannotManage);
    }

    @Test
    public void testFindAssignedViewersByClassId() {
        String classId = "class1";
        List<String> expectedUsers = List.of("viewer123");

        List<String> actualUsers = classAccessDao.findAssignedViewersByClassId(classId);

        assertNotNull(actualUsers);
        assertEquals(expectedUsers.size(), actualUsers.size());
        assertTrue(actualUsers.containsAll(expectedUsers));
    }

    @Test
    void deactivate() {
        classAccessDao.deactivate("class1");
    }

    @Test
    void deactivateUnknown() {
        assertThrows(NotFoundException.class, () -> classAccessDao.deactivate("nonexistent-class-id"));
    }

}
