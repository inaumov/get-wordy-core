package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.ThemeStatus;
import get.wordy.core.api.exception.DuplicateThemeException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.api.bean.Theme;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ThemeDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ThemeDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Theme> themeMapper =
            (rs, rowNum) -> new Theme(
                    rs.getInt("theme_id"),
                    rs.getString("name"),
                    rs.getString("notes"),
                    ThemeStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getInt("words_total")
            );

    public List<Theme> findAll(OwnerId ownerId) {

        String query = """
                select
                    th.*,
                    case
                        when th.status = 'DRAFT'
                            then coalesce(jsonb_array_length(th.candidate_words_draft), 0)
                        when th.status = 'READY'
                            then count(refs.word_id)
                        else 0
                    end as words_total
                from theme th
                left join theme_has_words refs
                    on refs.theme_id = th.theme_id
                where th.owner_id = :ownerId
                  and th.owner_type = :ownerType
                group by th.theme_id, th.name
                order by th.name
                """;

        return jdbcTemplate.query(
                query,
                ownerParams(ownerId),
                themeMapper
        );
    }

    public Optional<Theme> findById(OwnerId ownerId, int themeId) {
        String query = """
                select
                    th.*,
                    case
                        when th.status = 'DRAFT'
                            then coalesce(jsonb_array_length(th.candidate_words_draft), 0)
                        when th.status = 'READY'
                            then (
                                select count(*)
                                from theme_has_words refs
                                where refs.theme_id = th.theme_id
                            )
                        else 0
                    end as words_total
                from theme th
                where th.theme_id = :themeId
                  and th.owner_id = :ownerId
                  and th.owner_type = :ownerType
                """;

        try {

            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(
                            query,
                            ownerParams(ownerId)
                                    .addValue("themeId", themeId),
                            themeMapper
                    )
            );

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean hasAccess(OwnerId ownerId, int themeId) {
        String query = """
                SELECT COUNT(*)
                FROM theme
                WHERE theme_id = :themeId AND owner_id = :ownerId AND owner_type = :ownerType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("themeId", themeId)
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        Integer count = jdbcTemplate.queryForObject(query, params, Integer.class);
        return count != null && count > 0;
    }

    public Theme create(OwnerId ownerId, String name) {

        checkForNameCollision(ownerId, name);

        String query = """
                insert into theme(
                    owner_id,
                    owner_type,
                    name
                )
                values(
                    :ownerId,
                    :ownerType,
                    :name
                )
                returning
                    theme_id,
                    name,
                    notes,
                    status,
                    create_time,
                    0 as words_total
                """;

        try {

            return jdbcTemplate.queryForObject(
                    query,
                    ownerParams(ownerId)
                            .addValue("name", name),
                    themeMapper
            );

        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateThemeException(name);
        }
    }

    public Theme rename(OwnerId ownerId, int themeId, String name) {

        checkForNameCollision(ownerId, name);

        String query = """
                update theme th
                set name = :name
                where th.theme_id = :themeId
                returning
                    th.theme_id,
                    th.name,
                    th.notes,
                    th.status,
                    th.create_time,
                    (
                        select count(*)
                        from theme_has_words refs
                        where refs.theme_id = th.theme_id
                    ) as words_total
                """;

        return jdbcTemplate.queryForObject(
                query,
                new MapSqlParameterSource()
                        .addValue("themeId", themeId)
                        .addValue("name", name),
                themeMapper
        );
    }

    public Theme updateStatus(OwnerId ownerId, int themeId, ThemeStatus status) {
        String query = """
                update theme th
                set status = :status
                where th.theme_id = :themeId
                returning
                    th.theme_id,
                    th.name,
                    th.notes,
                    th.status,
                    th.create_time,
                    (
                        select count(*)
                        from theme_has_words refs
                        where refs.theme_id = th.theme_id
                    ) as words_total
                """;

        return jdbcTemplate.queryForObject(
                query,
                new MapSqlParameterSource()
                        .addValue("themeId", themeId)
                        .addValue("status", status.name()),
                themeMapper
        );
    }

    private void checkForNameCollision(OwnerId ownerId, String name) {

        String query = """
                select exists(
                select 1
                    from theme
                    where owner_id = :ownerId
                    and owner_type = :ownerType
                    and lower(name) = lower(:name)
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("name", name);

        Boolean exists = jdbcTemplate.queryForObject(
                query,
                params,
                Boolean.class
        );

        if (Boolean.TRUE.equals(exists)) {
            throw new DuplicateThemeException(name);
        }
    }

    public boolean delete(OwnerId ownerId, Integer themeId) {

        jdbcTemplate.update("""
                        delete from theme_has_words
                        where theme_id=:themeId
                        """,
                new MapSqlParameterSource(
                        "themeId",
                        themeId
                )
        );

        return jdbcTemplate.update("""
                        delete from theme
                        where theme_id=:themeId
                          and owner_id=:ownerId
                          and owner_type=:ownerType
                        """,
                ownerParams(ownerId)
                        .addValue(
                                "themeId",
                                themeId
                        )
        ) > 0;
    }

    public void addWordsToTheme(int themeId, Integer... wordIds) {
        bulkWordsUpdate("""
                insert into theme_has_words(theme_id,word_id)
                values(:themeId,:wordId)
                on conflict(theme_id,word_id)
                do nothing
                """, themeId, wordIds);
    }

    public void removeWordsFromTheme(int themeId, Integer... wordIds) {
        bulkWordsUpdate("""
                delete from theme_has_words
                where theme_id=:themeId
                and word_id=:wordId
                """, themeId, wordIds);
    }

    private void bulkWordsUpdate(String query, int themeId, Integer... wordIds) {

        if (wordIds == null || wordIds.length == 0) {
            return;
        }

        jdbcTemplate.batchUpdate(
                query,
                Arrays.stream(wordIds)
                        .map(id ->
                                new MapSqlParameterSource()
                                        .addValue("themeId", themeId)
                                        .addValue("wordId", id))
                        .toArray(MapSqlParameterSource[]::new)
        );
    }

    public Set<Integer> getWordIds(int themeId) {
        return jdbcTemplate.queryForStream("""
                        select word_id
                        from theme_has_words
                        where theme_id=:themeId
                        """,
                new MapSqlParameterSource("themeId", themeId), (rs, rowNum) -> rs.getInt("word_id")
        ).collect(Collectors.toSet());
    }

    private MapSqlParameterSource ownerParams(OwnerId ownerId) {
        return new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());
    }

    public void saveCandidateWordsJson(OwnerId ownerId, int themeId, String candidateWords) {

        String query = """
                update theme
                set candidate_words_draft = cast(:candidateWordsJson as jsonb)
                where theme_id = :themeId
                  and owner_id = :ownerId
                  and owner_type = :ownerType
                """;
        jdbcTemplate.update(
                query,
                ownerParams(ownerId)
                        .addValue("themeId", themeId)
                        .addValue(
                                "candidateWordsJson",
                                candidateWords
                        )
        );
    }

    public String getCandidateWordsJson(OwnerId ownerId, int themeId) {

        String query = """
                select candidate_words_draft
                from theme
                where theme_id = :themeId
                  and owner_id = :ownerId
                  and owner_type = :ownerType
                """;

        return jdbcTemplate.queryForObject(
                query,
                ownerParams(ownerId)
                        .addValue("themeId", themeId),
                String.class
        );
    }

    public void removeCandidateWord(OwnerId ownerId, int themeId, String lemma, String partOfSpeech) {
        String query = """
                update theme
                set candidate_words_draft = (
                    select coalesce(
                        jsonb_agg(elem),
                        '[]'::jsonb
                    )
                    from jsonb_array_elements(candidate_words_draft) elem
                    where not (
                        elem->>'lemma' = :lemma
                        and elem->>'partOfSpeech' = :partOfSpeech
                    )
                )
                where theme_id = :themeId
                  and owner_id = :ownerId
                  and owner_type = :ownerType
                """;

        jdbcTemplate.update(
                query,
                ownerParams(ownerId)
                        .addValue("themeId", themeId)
                        .addValue("lemma", lemma)
                        .addValue("partOfSpeech", partOfSpeech)
        );
    }

}
