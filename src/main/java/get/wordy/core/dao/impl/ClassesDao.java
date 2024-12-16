package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClassesDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ClassesDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ClassInfo> selectAllByOwnerId(OwnerId userId) {
        String query = """
                SELECT class_id, name, format, level, material, notes
                FROM classes
                WHERE owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        return jdbcTemplate.query(query, params, (rs, rowNum) -> new ClassInfo(
                rs.getString("class_id"),
                rs.getString("name"),
                rs.getString("format"),
                rs.getString("level"),
                rs.getString("material"),
                rs.getString("notes")
        ));
    }

    public ClassInfo insert(OwnerId userId, ClassInfo classInfo) {
        String query = """
                INSERT INTO classes (class_id, name, format, level, material, notes, owner_id, owner_type)
                VALUES (:classId, :name, :format, :level, :material, :notes, :ownerId, :ownerType)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classInfo.getClassId())
                .addValue("name", classInfo.getName())
                .addValue("format", classInfo.getFormat())
                .addValue("level", classInfo.getLevel())
                .addValue("material", classInfo.getMaterial())
                .addValue("notes", classInfo.getNotes())
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        jdbcTemplate.update(query, params);
        return classInfo;
    }

    public ClassInfo update(OwnerId userId, ClassInfo classInfo) {
        String query = """
                UPDATE classes
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
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        jdbcTemplate.update(query, params);
        return classInfo;
    }

    public int delete(OwnerId userId, String classId) {
        String query = """
                DELETE FROM classes
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        return jdbcTemplate.update(query, params);
    }

}
