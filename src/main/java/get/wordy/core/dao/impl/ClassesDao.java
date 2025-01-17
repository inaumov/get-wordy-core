package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.*;

@Repository
public class ClassesDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ClassesDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, ClassInfo> fetchAllClassesWithSchedules(OwnerId ownerId) {
        String query = """
                SELECT c.class_id, c.name, c.format, c.level, c.material, c.notes,
                       cs.day_of_week, cs.start_time, cs.end_time
                FROM class_info c
                JOIN class_schedule cs ON c.class_id = cs.class_id
                WHERE c.owner_id = :ownerId AND c.owner_type = :ownerType
                ORDER BY cs.day_of_week, cs.start_time
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params);

        // group schedules for the same class
        Map<String, ClassInfo> classMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            String classId = (String) row.get("class_id");
            ClassInfo classInfo = classMap.computeIfAbsent(classId, id -> new ClassInfo(
                    classId,
                    (String) row.get("name"),
                    (String) row.get("format"),
                    (String) row.get("level"),
                    (String) row.get("material"),
                    (String) row.get("notes"),
                    new ArrayList<>()
            ));

            // add schedule to the class
            classInfo.getSchedules().add(new ClassInfo.ClassSchedule(
                    ((String) row.get("day_of_week")).toLowerCase(),
                    ((Time) row.get("start_time")).toLocalTime(),
                    ((Time) row.get("end_time")).toLocalTime()
            ));
        }

        return classMap;
    }

    public ClassInfo insert(OwnerId ownerId, ClassInfo classInfo) {
        String classInsertQuery = """
                INSERT INTO class_info (class_id, name, format, level, material, notes, owner_id, owner_type)
                VALUES (:classId, :name, :format, :level, :material, :notes, :ownerId, :ownerType)
                """;
        MapSqlParameterSource classParams = new MapSqlParameterSource()
                .addValue("classId", classInfo.getClassId())
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("name", classInfo.getName())
                .addValue("format", classInfo.getFormat())
                .addValue("level", classInfo.getLevel())
                .addValue("material", classInfo.getMaterial())
                .addValue("notes", classInfo.getNotes());

        jdbcTemplate.update(classInsertQuery, classParams);

        String scheduleInsertQuery = """
                INSERT INTO class_schedule (class_id, day_of_week, start_time, end_time)
                VALUES (:classId, lower(:dayOfWeek), :startTime, :endTime)
                """;

        for (ClassInfo.ClassSchedule schedule : classInfo.getSchedules()) {
            MapSqlParameterSource scheduleParams = new MapSqlParameterSource()
                    .addValue("classId", classInfo.getClassId())
                    .addValue("dayOfWeek", schedule.getDayOfWeek())
                    .addValue("startTime", schedule.getStartTime())
                    .addValue("endTime", schedule.getEndTime());
            jdbcTemplate.update(scheduleInsertQuery, scheduleParams);
        }
        return classInfo;
    }

    public ClassInfo updateClassInfoOnly(OwnerId ownerId, ClassInfo classInfo) {
        String query = """
                UPDATE class_info
                SET name = :name, format = :format, level = :level, material = :material, notes = :notes
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classInfo.getClassId())
                .addValue("name", classInfo.getName())
                .addValue("format", classInfo.getFormat())
                .addValue("level", classInfo.getLevel())
                .addValue("material", classInfo.getMaterial())
                .addValue("notes", classInfo.getNotes())
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());
        jdbcTemplate.update(query, params);
        return classInfo;
    }

    public int delete(OwnerId ownerId, String classId) {
        String query = """
                DELETE FROM class_info
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());
        return jdbcTemplate.update(query, params);
    }

    public Optional<ClassInfo> selectById(OwnerId ownerId, String classId) {
        String classQuery = """
                SELECT class_id, name, format, level, material, notes
                FROM class_info
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;

        String scheduleQuery = """
                SELECT day_of_week, start_time, end_time
                FROM class_schedule
                WHERE class_id = :classId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(classQuery, params, (rs, rowNum) -> new ClassInfo(
                    rs.getString("class_id"),
                    rs.getString("name"),
                    rs.getString("format"),
                    rs.getString("level"),
                    rs.getString("material"),
                    rs.getString("notes"),
                    new ArrayList<>()
            ))).map(x -> {
                List<ClassInfo.ClassSchedule> schedules = jdbcTemplate.query(scheduleQuery, params, (rs, rowNum) -> new ClassInfo.ClassSchedule(
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime()
                ));
                x.setSchedules(schedules);
                return x;
            });
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}
