package get.wordy.core.dao.impl;

import get.wordy.core.api.id.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClassAccessDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ClassAccessDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // grant access to a viewer for a specific class
    public void grantAccess(String classId, String viewerId) {
        String query = """
                INSERT INTO class_access (class_id, viewer_id)
                VALUES (:classId, :viewerId)
                ON CONFLICT (class_id, viewer_id) DO NOTHING
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("viewerId", viewerId);

        jdbcTemplate.update(query, params);
    }

    // revoke access for a viewer to a specific class
    public void revokeAccess(String classId, String viewerId) {
        String query = """
                DELETE FROM class_access
                WHERE class_id = :classId AND viewer_id = :viewerId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("viewerId", viewerId);

        jdbcTemplate.update(query, params);
    }

    // check if an admin has access to a class
    public boolean hasFullAccess(String classId, OwnerId adminId) {
        String query = """
                SELECT COUNT(*)
                FROM class_info
                WHERE class_id = :classId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("ownerId", adminId.ownerId())
                .addValue("ownerType", adminId.ownerType());

        Integer count = jdbcTemplate.queryForObject(query, params, Integer.class);
        return count != null && count > 0;
    }

    // check if a viewer has access to a class
    public boolean hasViewAccess(String classId, String viewerId) {
        String query = """
                SELECT COUNT(*)
                FROM class_access
                WHERE class_id = :classId AND viewer_id = :viewerId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId)
                .addValue("viewerId", viewerId);

        Integer count = jdbcTemplate.queryForObject(query, params, Integer.class);
        return count != null && count > 0;
    }

    public Map<String, Boolean> findAccessibleClasses(String viewerId) {
        String query = """
                SELECT class_id, is_active
                FROM class_access
                WHERE viewer_id = :viewerId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("viewerId", viewerId);

        return jdbcTemplate.query(query, params, rs -> {
            Map<String, Boolean> resultMap = new HashMap<>();
            while (rs.next()) {
                String classId = rs.getString("class_id");
                boolean isActive = rs.getBoolean("is_active");
                resultMap.put(classId, isActive);
            }
            return resultMap;
        });
    }

    public List<String> findAssignedViewersByClassId(String classId) {
        String query = """
                SELECT viewer_id
                FROM class_access
                WHERE class_id = :classId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("classId", classId);

        return jdbcTemplate.queryForList(query, params, String.class);
    }

}
