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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {ClassesDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:classes.sql") // load predefined inserts
public class ClassesDaoTest {

    @Autowired
    private ClassesDao classesDao;

    @Test
    public void testFetchAllClassesWithSchedules() {
        // given
        OwnerId ownerId = new OwnerId("user123", "user");

        // when
        Map<String, ClassInfo> groupedClasses = classesDao.fetchAllClassesWithSchedules(ownerId);

        // then
        assertNotNull(groupedClasses);
        assertEquals(12, groupedClasses.size());
        assertTrue(groupedClasses.containsKey("class1"));
        assertTrue(groupedClasses.containsKey("class3"));

        // Validate ClassInfo for "class1"
        ClassInfo class1 = groupedClasses.get("class1");
        assertNotNull(class1);
        assertEquals("Beginner English", class1.getName());
        assertEquals(2, class1.getSchedules().size()); // Mon, Wed

        ClassInfo.ClassSchedule mondaySchedule = class1.getSchedules().stream()
                .filter(s -> "mon".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(mondaySchedule);
        assertEquals(LocalTime.of(9, 0), mondaySchedule.getStartTime());
        assertEquals(LocalTime.of(10, 0), mondaySchedule.getEndTime());

        // Validate ClassInfo for "class3"
        ClassInfo class3 = groupedClasses.get("class3");
        assertNotNull(class3);
        assertEquals("Advanced English", class3.getName());
        assertEquals(2, class3.getSchedules().size()); // Fri, Sat

        ClassInfo.ClassSchedule fridaySchedule = class3.getSchedules().stream()
                .filter(s -> "fri".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(fridaySchedule);
        assertEquals(LocalTime.of(10, 0), fridaySchedule.getStartTime());
        assertEquals(LocalTime.of(11, 30), fridaySchedule.getEndTime());
    }

    @Test
    public void testInsertClassInfoWithSchedules() {
        OwnerId ownerId = new OwnerId("user123", "user");

        ClassInfo.ClassSchedule schedule1 = new ClassInfo.ClassSchedule("Mon", LocalTime.of(10, 0), LocalTime.of(10, 50));
        ClassInfo.ClassSchedule schedule2 = new ClassInfo.ClassSchedule("Fri", LocalTime.of(14, 0), LocalTime.of(14, 50));
        ClassInfo classInfo = new ClassInfo("class13", "Tower 101", "Lecture", "Beginner", "Algebra", "None", List.of(schedule1, schedule2));

        classesDao.insert(ownerId, classInfo);

        Map<String, ClassInfo> groupedClasses = classesDao.fetchAllClassesWithSchedules(ownerId);

        assertTrue(groupedClasses.containsKey("class13"));

        ClassInfo insertedClass = groupedClasses.get("class13");
        assertEquals("Tower 101", insertedClass.getName());
        assertEquals("Lecture", insertedClass.getFormat());
        assertEquals("Beginner", insertedClass.getLevel());
        assertEquals(2, insertedClass.getSchedules().size());

        // assertions to verify the class and schedules are inserted correctly

        ClassInfo.ClassSchedule mondaySchedule = insertedClass.getSchedules().stream()
                .filter(s -> "mon".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(mondaySchedule);
        assertEquals(schedule1.getStartTime(), mondaySchedule.getStartTime());
        assertEquals(schedule1.getEndTime(), mondaySchedule.getEndTime());

        ClassInfo.ClassSchedule fridaySchedule = insertedClass.getSchedules().stream()
                .filter(s -> "fri".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(fridaySchedule);
        assertEquals(schedule2.getStartTime(), fridaySchedule.getStartTime());
        assertEquals(schedule2.getEndTime(), fridaySchedule.getEndTime());
    }

    @Test
    public void testDelete() {
        OwnerId ownerId = new OwnerId("user123", "user");

        classesDao.delete(ownerId, "class1");

        Map<String, ClassInfo> results = classesDao.fetchAllClassesWithSchedules(ownerId);
        assertEquals(11, results.size()); // 12 classes initially, 1 deleted

        assertFalse(results.containsKey("class1"));
    }

    @Test
    public void testSelectById() {
        OwnerId ownerId = new OwnerId("user123", "user");

        // Fetch data and verify it is present
        Optional<ClassInfo> classInfo = classesDao.selectById(ownerId, "class1");
        assertTrue(classInfo.isPresent());

        ClassInfo entity = classInfo.get();
        assertEquals("Beginner English", entity.getName());
        assertEquals("Lecture", entity.getFormat());
        assertEquals("Beginner", entity.getLevel());
        assertEquals("Grammar Basics", entity.getMaterial());
        assertEquals("Morning class", entity.getNotes());
        assertEquals(2, entity.getSchedules().size()); // Mon, Wed
    }

    @Test
    public void testSelectByIdNotFound() {
        OwnerId ownerId = new OwnerId("user123", "user");

        // Fetch data and verify it is not present
        Optional<ClassInfo> classInfo = classesDao.selectById(ownerId, "class999");
        assertTrue(classInfo.isEmpty());
    }

}
