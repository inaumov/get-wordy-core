package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.bean.ClassSchedule;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.*;

@Repository
public class ClassesDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ClassesDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, ClassInfo> fetchAllClasses(OwnerId ownerId) {
        String query = """
                SELECT c.class_id, c.name, c.format, c.level, c.material, c.notes, c.is_repeatable, c.end_date, c.is_active,
                       cs.day_of_week, cs.start_time, cs.end_time
                FROM class_info c
                LEFT JOIN class_schedule cs ON c.class_id = cs.class_id
                WHERE c.owner_id = :ownerId AND c.owner_type = :ownerType
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params);

        // group schedules for the same class
        Map<String, ClassInfo> classMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            String classId = (String) row.get("class_id");
            Boolean isRepeatable = (Boolean) row.get("is_repeatable");
            ClassInfo classInfo = classMap.computeIfAbsent(classId, id -> new ClassInfo(
                    classId,
                    (String) row.get("name"),
                    (String) row.get("format"),
                    (String) row.get("level"),
                    (String) row.get("material"),
                    (String) row.get("notes"),
                    isRepeatable
            ));
            Boolean isActive = (Boolean) row.get("is_active");
            classInfo.setIsActive(isActive);
            if (!isActive) {
                continue;
            }
            // add schedule to the class if repeatable && active
            if (isRepeatable) {
                classInfo.getTimeSlots().add(new ClassSchedule(
                        ((String) row.get("day_of_week")),
                        ((Time) row.get("start_time")).toLocalTime(),
                        ((Time) row.get("end_time")).toLocalTime()
                ));
            } else {
                // handle onetime activities
                Date date = (Date) row.get("end_date");
                LocalDate endDate = date != null ? date.toLocalDate() : null;
                classInfo.setEndDate(endDate);
            }
        }

        return classMap;
    }

    public ClassInfo insert(OwnerId ownerId, ClassInfo classInfo) {
        String insertQuery = """
                INSERT INTO class_info (class_id, name, format, level, material, notes, owner_id, owner_type, is_repeatable, end_date)
                VALUES (:classId, :name, :format, :level, :material, :notes, :ownerId, :ownerType, :isRepeatable, :endDate)
                RETURNING is_active
                """;
        MapSqlParameterSource classParams = new MapSqlParameterSource()
                .addValue("classId", classInfo.getClassId())
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("name", classInfo.getName())
                .addValue("format", classInfo.getFormat())
                .addValue("level", classInfo.getLevel())
                .addValue("material", classInfo.getMaterial())
                .addValue("notes", classInfo.getNotes())
                .addValue("isRepeatable", classInfo.getIsRepeatable())
                .addValue("endDate", classInfo.getEndDate());

        Boolean isActive = jdbcTemplate.queryForObject(insertQuery, classParams, Boolean.class);
        // handle default database value
        classInfo.setIsActive(Boolean.TRUE.equals(isActive));

        if (!classInfo.getIsRepeatable()) {
            return classInfo;
        }
        insertSchedule(classInfo);

        return classInfo;
    }

    public ClassInfo update(OwnerId ownerId, ClassInfo classInfo) {
        String updateQuery = """
                UPDATE class_info
                SET name = :name, format = :format, level = :level, material = :material, notes = :notes, is_repeatable = :isRepeatable, end_date = :endDate
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                RETURNING is_active
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classInfo.getClassId())
                .addValue("name", classInfo.getName())
                .addValue("format", classInfo.getFormat())
                .addValue("level", classInfo.getLevel())
                .addValue("material", classInfo.getMaterial())
                .addValue("notes", classInfo.getNotes())
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("isRepeatable", classInfo.getIsRepeatable())
                .addValue("endDate", classInfo.getEndDate());

        Boolean isActive = jdbcTemplate.queryForObject(updateQuery, params, Boolean.class);
        classInfo.setIsActive(Boolean.TRUE.equals(isActive));
        // delete actual schedule
        String clean = """
                DELETE FROM class_schedule
                WHERE class_id = :classId
                """;
        jdbcTemplate.update(clean, params);

        // does not save schedules when not repeatable
        if (!classInfo.getIsRepeatable()) {
            return classInfo;
        }

        // insert new
        insertSchedule(classInfo);

        return classInfo;
    }

    private void insertSchedule(ClassInfo classInfo) {
        if (CollectionUtils.isEmpty(classInfo.getTimeSlots())) {
            return;
        }
        String scheduleInsertQuery = """
                INSERT INTO class_schedule (class_id, day_of_week, start_time, end_time)
                VALUES (:classId, :dayOfWeek, :startTime, :endTime)
                """;
        for (ClassSchedule schedule : classInfo.getTimeSlots()) {
            MapSqlParameterSource scheduleParams = new MapSqlParameterSource()
                    .addValue("classId", classInfo.getClassId())
                    .addValue("dayOfWeek", schedule.getDayOfWeek())
                    .addValue("startTime", schedule.getStartTime())
                    .addValue("endTime", schedule.getEndTime());
            jdbcTemplate.update(scheduleInsertQuery, scheduleParams);
        }
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
                SELECT class_id, name, format, level, material, notes, is_repeatable, end_date, is_active
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
            return Optional.ofNullable(jdbcTemplate.queryForObject(classQuery, params, (rs, rowNum) -> {
                boolean isRepeatable = rs.getBoolean("is_repeatable");
                ClassInfo classInfo = new ClassInfo(
                        rs.getString("class_id"),
                        rs.getString("name"),
                        rs.getString("format"),
                        rs.getString("level"),
                        rs.getString("material"),
                        rs.getString("notes"),
                        isRepeatable
                );
                boolean isActive = rs.getBoolean("is_active");
                classInfo.setIsActive(isActive);
                if (isActive && !isRepeatable) {
                    Date date = rs.getDate("end_date");
                    classInfo.setEndDate(date != null ? date.toLocalDate() : null);
                    return classInfo;
                }
                return classInfo;
            })).map(x -> {
                if (x.getIsActive() && x.getIsRepeatable()) {
                    List<ClassSchedule> schedules = jdbcTemplate.query(scheduleQuery, params, (rs, rowNum) -> new ClassSchedule(
                            rs.getString("day_of_week"),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime()
                    ));
                    x.setTimeSlots(schedules);
                }
                return x;
            });
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<ClassInfo> selectByIds(Collection<String> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            // Return empty list if no classIds are provided
            return new ArrayList<>();
        }

        String query = """
                SELECT c.class_id, c.name, c.format, c.level, c.material, c.notes, c.is_repeatable, c.end_date, c.is_active,
                       cs.day_of_week, cs.start_time, cs.end_time
                FROM class_info c
                LEFT JOIN class_schedule cs ON c.class_id = cs.class_id
                WHERE c.class_id IN (:classIds)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classIds", classIds);

        // Create a list to hold the final classInfo objects
        List<ClassInfo> classInfos = new ArrayList<>();
        Map<String, ClassInfo> classInfoMap = new HashMap<>();

        // Query the database to get the class info and schedules in one go
        jdbcTemplate.query(query, params, (rs, rowNum) -> {
            String classId = rs.getString("class_id");

            // Create ClassInfo object only once per class_id
            if (!classInfoMap.containsKey(classId)) {
                ClassInfo classInfo = new ClassInfo(
                        classId,
                        rs.getString("name"),
                        rs.getString("format"),
                        rs.getString("level"),
                        rs.getString("material"),
                        rs.getString("notes"),
                        rs.getBoolean("is_repeatable")
                );
                classInfo.setIsActive(rs.getBoolean("is_active"));
                classInfoMap.put(classId, classInfo);
                classInfos.add(classInfo);
            }

            // Add the schedule to the classInfo if available
            ClassInfo classInfo = classInfoMap.get(classId);
            if (classInfo.getIsRepeatable()) {
                String dayOfWeek = rs.getString("day_of_week");
                if (dayOfWeek != null) {
                    ClassSchedule schedule = new ClassSchedule(
                            dayOfWeek,
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime()
                    );
                    classInfo.getTimeSlots().add(schedule);
                }
            } else {
                LocalDate endDate = rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null;
                classInfo.setEndDate(endDate);
            }

            return null;
        });

        return classInfos;
    }

    public void resetSchedule(OwnerId ownerId, String classId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        // delete any actual schedule
        String clean = """
                DELETE FROM class_schedule
                WHERE class_id = :classId
                """;
        int updated = jdbcTemplate.update(clean, params);
        if (updated == 0) {
            throw new NotFoundException("no class_schedule record found for classId: " + classId);
        }

        // reset end_date
        String query = """
                UPDATE class_info
                SET end_date = NULL
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        jdbcTemplate.update(query, params);
    }

    public void updateActivation(OwnerId ownerId, String classId, boolean isActive) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("isActive", isActive);

        String query = """
                UPDATE class_info
                SET is_active = :isActive
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;

        int updated = jdbcTemplate.update(query, params);
        if (updated == 0) {
            throw new NotFoundException("no class_info record found for classId: " + classId);
        }
    }

}
