package get.wordy.dao.impl;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.bean.ClassSchedule;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.NotFoundException;
import get.wordy.core.dao.impl.ClassesDao;
import get.wordy.dao.config.SpringJdbcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {ClassesDao.class, SpringJdbcConfig.class})
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:classes.sql") // load predefined inserts
@Rollback
public class ClassesDaoTest {

    @Autowired
    private ClassesDao classesDao;

    @Test
    public void testFetchAllClasses() {
        // given
        OwnerId ownerId = new OwnerId("user123", "user");

        // when
        Map<String, ClassInfo> groupedClasses = classesDao.fetchAllClasses(ownerId);

        // then
        assertNotNull(groupedClasses);
        assertEquals(14, groupedClasses.size());
        assertTrue(groupedClasses.containsKey("class1"));
        assertTrue(groupedClasses.containsKey("class3"));
        // no schedule
        assertTrue(groupedClasses.containsKey("class111"));
        assertTrue(groupedClasses.containsKey("class112"));

        // Validate ClassInfo for "class1"
        ClassInfo class1 = groupedClasses.get("class1");
        assertNotNull(class1);
        assertEquals("Beginner English", class1.getName());
        assertTrue(class1.getIsRepeatable());
        assertTrue(class1.getIsActive());
        assertEquals(2, class1.getTimeSlots().size()); // Mon, Wed

        ClassSchedule mondaySchedule = class1.getTimeSlots().stream()
                .filter(s -> "Mon".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(mondaySchedule);
        assertEquals(LocalTime.of(9, 0), mondaySchedule.getStartTime());
        assertEquals(LocalTime.of(10, 0), mondaySchedule.getEndTime());

        // Validate ClassInfo for "class3"
        ClassInfo class3 = groupedClasses.get("class3");
        assertNotNull(class3);
        assertEquals("Advanced English", class3.getName());
        assertTrue(class3.getIsRepeatable());
        assertTrue(class3.getIsActive());
        assertEquals(2, class3.getTimeSlots().size()); // Fri, Sat

        ClassSchedule fridaySchedule = class3.getTimeSlots().stream()
                .filter(s -> "Fri".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(fridaySchedule);
        assertEquals(LocalTime.of(10, 0), fridaySchedule.getStartTime());
        assertEquals(LocalTime.of(11, 30), fridaySchedule.getEndTime());

        // Validate onetime class
        ClassInfo class111 = groupedClasses.get("class111");
        assertNotNull(class111);
        assertEquals("Test onetime", class111.getName());
        assertFalse(class111.getIsRepeatable());
        assertTrue(class111.getIsActive());
        assertTrue(class111.getTimeSlots().isEmpty());
        assertTrue(class111.getEndDate().isEqual(LocalDate.of(2025, 5, 31)));

        // Validate no scheduled class
        ClassInfo class112 = groupedClasses.get("class112");
        assertNotNull(class112);
        assertEquals("Test no schedule", class112.getName());
        assertFalse(class112.getIsRepeatable());
        assertTrue(class112.getIsActive());
        assertTrue(class112.getTimeSlots().isEmpty());
        assertNull(class112.getEndDate());
    }

    @Test
    public void testInsertClassInfo_whenWithSchedules() {
        OwnerId ownerId = new OwnerId("user123", "user");

        ClassSchedule schedule1 = new ClassSchedule("Mon", LocalTime.of(10, 0), LocalTime.of(10, 50));
        ClassSchedule schedule2 = new ClassSchedule("Fri", LocalTime.of(14, 0), LocalTime.of(14, 50));
        ClassInfo classInfo = new ClassInfo("class13", "Tower 101", "Lecture", "Beginner", "Algebra", "None", true);
        classInfo.setTimeSlots(List.of(schedule1, schedule2));

        ClassInfo newClass = classesDao.insert(ownerId, classInfo);
        assertTrue(newClass.getIsActive());

        Map<String, ClassInfo> groupedClasses = classesDao.fetchAllClasses(ownerId);

        assertTrue(groupedClasses.containsKey("class13"));

        ClassInfo insertedClass = groupedClasses.get("class13");
        assertEquals("Tower 101", insertedClass.getName());
        assertEquals("Lecture", insertedClass.getFormat());
        assertEquals("Beginner", insertedClass.getLevel());
        assertTrue(insertedClass.getIsActive());
        assertTrue(insertedClass.getIsRepeatable());
        assertEquals(2, insertedClass.getTimeSlots().size());

        // assertions to verify the class and schedules are inserted correctly

        ClassSchedule mondaySchedule = insertedClass.getTimeSlots().stream()
                .filter(s -> "Mon".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(mondaySchedule);
        assertEquals(schedule1.getStartTime(), mondaySchedule.getStartTime());
        assertEquals(schedule1.getEndTime(), mondaySchedule.getEndTime());

        ClassSchedule fridaySchedule = insertedClass.getTimeSlots().stream()
                .filter(s -> "Fri".equals(s.getDayOfWeek()))
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

        Map<String, ClassInfo> results = classesDao.fetchAllClasses(ownerId);
        assertEquals(13, results.size()); // 14 classes initially, 1 deleted

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
        assertTrue(entity.getIsRepeatable());
        assertTrue(entity.getIsActive());
        assertEquals(2, entity.getTimeSlots().size()); // Mon, Wed
    }

    @Test
    public void testSelectByIdNotFound() {
        OwnerId ownerId = new OwnerId("user123", "user");

        // Fetch data and verify it is not present
        Optional<ClassInfo> classInfo = classesDao.selectById(ownerId, "class999");
        assertTrue(classInfo.isEmpty());
    }

    @Test
    void selectByIds() {
        List<ClassInfo> classInfos = classesDao.selectByIds(List.of("class1", "class12"));
        assertEquals(2, classInfos.size());

        ClassInfo first = classInfos.getFirst();
        assertEquals("Beginner English", first.getName());
        assertEquals("Lecture", first.getFormat());
        assertEquals("Beginner", first.getLevel());
        assertEquals("Grammar Basics", first.getMaterial());
        assertEquals("Morning class", first.getNotes());
        assertTrue(first.getIsRepeatable());
        assertTrue(first.getIsActive());
        assertEquals(2, first.getTimeSlots().size()); // Mon, Wed
    }

    @Test
    void updateClassInfo_whenNonRepeatable() {
        OwnerId ownerId = new OwnerId("user123", "user");
        ClassInfo classInfo = new ClassInfo("class11", "Tower AAA", "VIP Online", null, null, "Some notes", false);
        classInfo.setEndDate(LocalDate.now());

        ClassInfo updated = classesDao.update(ownerId, classInfo);
        assertNotNull(updated);
    }

    @Test
    void updateClassInfo_whenHasSchedule() {
        OwnerId ownerId = new OwnerId("user123", "user");
        ClassInfo classInfo = new ClassInfo("class11", "Tower AAA", "VIP Online", null, null, "Some notes", true);
        ClassSchedule schedule1 = new ClassSchedule("Mon", LocalTime.of(10, 0), LocalTime.of(10, 50));
        ClassSchedule schedule2 = new ClassSchedule("Fri", LocalTime.of(14, 0), LocalTime.of(14, 50));
        classInfo.setTimeSlots(List.of(schedule1, schedule2));

        ClassInfo updated = classesDao.update(ownerId, classInfo);
        assertNotNull(updated);
    }

    @Test
    void deactivate() {
        OwnerId ownerId = new OwnerId("user123", "user");
        String class11 = "class11";
        classesDao.updateActivation(ownerId, class11, false);
        classesDao.resetSchedule(ownerId, class11);
        ClassInfo classInfo = classesDao.selectById(ownerId, class11)
                .orElseThrow();
        assertNull(classInfo.getEndDate());
        assertTrue(classInfo.getTimeSlots().isEmpty());
        assertFalse(classInfo.getIsActive());
    }

    @Test
    void deactivateUnknown() {
        OwnerId ownerId = new OwnerId("user123", "user");
        assertThrows(NotFoundException.class, () -> classesDao.resetSchedule(ownerId, "nonexistent-class-id"));
    }

}
