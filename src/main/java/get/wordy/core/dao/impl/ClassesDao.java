package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.ClassDetails;
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

    public List<ClassDetails> selectAllByOwnerId(OwnerId userId) {
        String query = """
                SELECT class_id, name, format, level, material, notes
                FROM classes
                WHERE owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        return jdbcTemplate.query(query, params, (rs, rowNum) -> new ClassDetails(
                rs.getString("class_id"),
                rs.getString("name"),
                rs.getString("format"),
                rs.getString("level"),
                rs.getString("material"),
                rs.getString("notes")
        ));
    }

    public ClassDetails insert(OwnerId userId, ClassDetails classDetails) {
        String query = """
                INSERT INTO classes (class_id, name, format, level, material, notes, owner_id, owner_type)
                VALUES (:classId, :name, :format, :level, :material, :notes, :ownerId, :ownerType)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classDetails.getClassId())
                .addValue("name", classDetails.getName())
                .addValue("format", classDetails.getFormat())
                .addValue("level", classDetails.getLevel())
                .addValue("material", classDetails.getMaterial())
                .addValue("notes", classDetails.getNotes())
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        jdbcTemplate.update(query, params);
        return classDetails;
    }

    public ClassDetails update(OwnerId userId, ClassDetails classDetails) {
        String query = """
                UPDATE classes
                SET name = :name, format = :format, level = :level, material = :material, notes = :notes
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classDetails.getClassId())
                .addValue("name", classDetails.getName())
                .addValue("format", classDetails.getFormat())
                .addValue("level", classDetails.getLevel())
                .addValue("material", classDetails.getMaterial())
                .addValue("notes", classDetails.getNotes())
                .addValue("ownerId", userId.ownerId())
                .addValue("ownerType", userId.ownerType());
        jdbcTemplate.update(query, params);
        return classDetails;
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
