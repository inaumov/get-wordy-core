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
    public void testGroupClassesByDay() {
        // given
        OwnerId ownerId = new OwnerId("user123", "user");

        // when
        Map<String, List<ClassInfo>> groupedClasses = classesDao.groupClassesByDay(ownerId);

        // then
        assertNotNull(groupedClasses);
        assertTrue(groupedClasses.containsKey("Mon"));
        assertTrue(groupedClasses.containsKey("Fri"));

        // validate classes grouped under Monday
        // "Beginner English", "Business English" and "Conversational English"
        List<ClassInfo> mondayClasses = groupedClasses.get("Mon");
        assertEquals(3, mondayClasses.size());

        ClassInfo class1 = mondayClasses.stream()
                .filter(c -> c.getClassId().equals("class1"))
                .findFirst()
                .orElse(null);
        assertNotNull(class1);
        assertEquals("Beginner English", class1.getName());
        assertEquals(2, class1.getSchedules().size()); // Mon, Wed

        // validate a single class schedule
        ClassInfo.ClassSchedule schedule1 = class1.getSchedules().stream()
                .filter(s -> "Mon".equals(s.getDayOfWeek()))
                .findFirst()
                .orElse(null);
        assertNotNull(schedule1);
        assertEquals(LocalTime.of(9, 0), schedule1.getStartTime());
        assertEquals(LocalTime.of(10, 0), schedule1.getEndTime());

        // validate classes grouped under Friday
        // "Advanced English" and "Business English", "Pronunciation Practice"
        List<ClassInfo> fridayClasses = groupedClasses.get("Fri");
        assertEquals(3, fridayClasses.size());

        Set<String> friClasses = Set.of("class3", "class4", "class11");
        fridayClasses
                .forEach(classInfo -> assertTrue(friClasses.contains(classInfo.getClassId())));
    }

    @Test
    public void testInsertClassInfoWithSchedules() {
        OwnerId ownerId = new OwnerId("user123", "user");

        ClassInfo.ClassSchedule schedule1 = new ClassInfo.ClassSchedule("Mon", LocalTime.of(10, 0), LocalTime.of(10, 50));
        ClassInfo.ClassSchedule schedule2 = new ClassInfo.ClassSchedule("Fri", LocalTime.of(14, 0), LocalTime.of(14, 50));
        ClassInfo classInfo = new ClassInfo("class13", "Tower 101", "Lecture", "Beginner", "Algebra", "None", List.of(schedule1, schedule2));

        classesDao.insert(ownerId, classInfo);

        // Assertions to verify the class and schedules are inserted correctly
        Map<String, List<ClassInfo>> groupedClasses = classesDao.groupClassesByDay(ownerId);

        assertTrue(groupedClasses.containsKey("Mon"));
        assertTrue(groupedClasses.containsKey("Fri"));

        List<ClassInfo> mondayClasses = groupedClasses.get("Mon");
        assertEquals(1, mondayClasses
                .stream()
                .filter(c -> c.getClassId().equals("class13")).count());

        ClassInfo insertedClass = mondayClasses.stream()
                .filter(c -> c.getClassId().equals("class13"))
                .findFirst()
                .orElseThrow();
        assertEquals("Tower 101", insertedClass.getName());
        assertEquals("Lecture", insertedClass.getFormat());
        assertEquals("Beginner", insertedClass.getLevel());
        assertEquals(2, insertedClass.getSchedules().size());
    }

    @Test
    public void testDelete() {
        OwnerId ownerId = new OwnerId("user123", "user");

        classesDao.delete(ownerId, "class1");

        Map<String, List<ClassInfo>> groupedClasses = classesDao.groupClassesByDay(ownerId);
//        assertEquals(11, results.size()); // 12 classes initially, 1 deleted

        groupedClasses.values().forEach(classList ->
                assertFalse(classList.stream().anyMatch(c -> c.getClassId().equals("class1")))
        );
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
