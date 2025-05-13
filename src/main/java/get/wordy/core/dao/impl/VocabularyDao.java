package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.id.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Access Object (DAO) for managing vocabularies and their associated words.
 * <p>
 * This class provides methods for performing CRUD operations on vocabularies,
 * managing their relationships with words, and handling specific fields like
 * {@code picture_url} and {@code words_total}.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>CRUD Operations:</b>
 *     <ul>
 *       <li>{@code selectAllByOwnerId(OwnerId ownerId)}: Retrieves all vocabularies for a given owner.</li>
 *       <li>{@code selectById(int vocabId)}: Fetches a specific vocabulary by its ID.</li>
 *       <li>{@code insert(OwnerId ownerId, String name, String pictureUrl)}: Inserts a new vocabulary with the specified details.</li>
 *       <li>{@code rename(int vocabId, String name)}: Updates the name of a vocabulary.</li>
 *       <li>{@code updatePicture(int vocabId, String pictureUrl)}: Updates the picture URL of a vocabulary.</li>
 *       <li>{@code updateIsShared(int vocabId, boolean isShared)}: Updates the shared status of a vocabulary.</li>
 *       <li>{@code deleteVocabularyById(int vocabId)}: Deletes a vocabulary and its associated words.</li>
 *     </ul>
 *   </li>
 *   <li><b>Word Management:</b>
 *     <ul>
 *       <li>{@code addWordsToVocabulary(int vocabId, Set<Integer> wordRefs)}: Associates multiple words with a vocabulary.</li>
 *       <li>{@code removeWordsFromVocabulary(int vocabId, Set<Integer> wordRefs)}: Removes multiple words from a vocabulary.</li>
 *       <li>{@code getWordRefs(int vocabId)}: Retrieves all word references associated with a vocabulary.</li>
 *     </ul>
 *   </li>
 *   <li><b>Query Features:</b>
 *     <ul>
 *       <li>Handles {@code words_total}, a calculated field representing the total number of words in a vocabulary.</li>
 *       <li>Manages the {@code picture_url} field for storing image URLs associated with vocabularies.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Note:</h2>
 * Methods return results in a structured and efficient manner. Batch updates for word management
 * are implemented for performance, and transaction management is recommended for safety.
 *
 * @see Vocabulary
 * @see OwnerId
 */
@Repository
public class VocabularyDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public VocabularyDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Vocabulary> selectAllByOwnerId(OwnerId ownerId) {
        String query = """
                SELECT vocabs.vocab_id AS vocab_id,
                       vocabs.name,
                       vocabs.picture_url,
                       vocabs.is_shared,
                       vocabs.create_time,
                       COUNT(refs.word_ref) AS words_total
                FROM vocabularies vocabs
                LEFT JOIN vocab_has_words refs ON vocabs.vocab_id = refs.vocab_id
                WHERE vocabs.owner_id = :ownerId AND vocabs.owner_type = :ownerType
                GROUP BY vocabs.vocab_id, vocabs.name, vocabs.picture_url, vocabs.is_shared
                ORDER BY vocabs.name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType());

        return jdbcTemplate.query(query, params, (rs, rowNum) -> processRecord(rs));
    }

    public Optional<Vocabulary> selectById(int vocabId) {
        String query = """
                SELECT vocabs.vocab_id AS vocab_id,
                       vocabs.name,
                       vocabs.picture_url,
                       vocabs.is_shared,
                       vocabs.create_time,
                       COUNT(refs.word_ref) AS words_total
                FROM vocabularies vocabs
                LEFT JOIN vocab_has_words refs ON vocabs.vocab_id = refs.vocab_id
                WHERE vocabs.vocab_id = :vocabId
                GROUP BY vocabs.vocab_id, vocabs.name, vocabs.picture_url, vocabs.is_shared
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("vocabId", vocabId);

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> processRecord(rs)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Vocabulary insert(OwnerId ownerId, Vocabulary vocabulary) {
        String query = """
                INSERT INTO vocabularies (owner_id, owner_type, name, picture_url, is_shared, create_time)
                VALUES (:ownerId, :ownerType, :name, :pictureUrl, false, current_timestamp)
                RETURNING vocab_id, name, picture_url, is_shared, 0 AS words_total, create_time
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId.ownerId())
                .addValue("ownerType", ownerId.ownerType())
                .addValue("name", vocabulary.getName())
                .addValue("pictureUrl", vocabulary.getPictureUrl());

        return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> processRecord(rs));
    }

    public int rename(int vocabId, String name) {
        String query = "UPDATE vocabularies SET name = :name WHERE vocab_id = :vocabId";
        return jdbcTemplate.update(query, new MapSqlParameterSource()
                .addValue("vocabId", vocabId)
                .addValue("name", name));
    }

    public int updatePicture(int vocabId, String pictureUrl) {
        String query = "UPDATE vocabularies SET picture_url = :pictureUrl WHERE vocab_id = :vocabId";
        return jdbcTemplate.update(query, new MapSqlParameterSource()
                .addValue("vocabId", vocabId)
                .addValue("pictureUrl", pictureUrl));
    }

    public int updateIsShared(int vocabId, boolean isShared) {
        String query = "UPDATE vocabularies SET is_shared = :isShared AND create_time = NOW() WHERE vocab_id = :vocabId";
        return jdbcTemplate.update(query, new MapSqlParameterSource()
                .addValue("vocabId", vocabId)
                .addValue("isShared", isShared));
    }

    public void addWordsToVocabulary(int vocabId, Set<Integer> wordRefs) {
        String query = """
                INSERT INTO vocab_has_words (vocab_id, word_ref)
                VALUES (:vocabId, :wordRef)
                """;
        jdbcTemplate.batchUpdate(query, wordRefs.stream()
                .map(wordRef -> new MapSqlParameterSource()
                        .addValue("vocabId", vocabId)
                        .addValue("wordRef", wordRef))
                .toArray(MapSqlParameterSource[]::new));
    }

    public void removeWordsFromVocabulary(int vocabId, Set<Integer> wordRefs) {
        String query = """
                DELETE FROM vocab_has_words
                WHERE vocab_id = :vocabId AND word_ref = :wordRef
                """;

        jdbcTemplate.batchUpdate(query, wordRefs.stream()
                .map(wordRef -> new MapSqlParameterSource()
                        .addValue("vocabId", vocabId)
                        .addValue("wordRef", wordRef))
                .toArray(MapSqlParameterSource[]::new));
    }

    public Set<Integer> getWordRefs(int vocabId) {
        String query = """
                SELECT word_ref
                FROM vocab_has_words
                WHERE vocab_id = :vocabId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("vocabId", vocabId);
        return jdbcTemplate.queryForStream(query, params, (rs, rowNum) -> rs.getInt("word_ref"))
                .collect(Collectors.toSet());
    }

    public int deleteVocabularyById(int vocabId) {
        // Delete related entries first
        String deleteWordsRefs = "DELETE FROM vocab_has_words WHERE vocab_id = :vocabId";
        jdbcTemplate.update(deleteWordsRefs, new MapSqlParameterSource("vocabId", vocabId));

        // Delete the vocabulary entry
        String deleteVocabulary = "DELETE FROM vocabularies WHERE vocab_id = :vocabId";
        return jdbcTemplate.update(deleteVocabulary, new MapSqlParameterSource("vocabId", vocabId));
    }

    private static Vocabulary processRecord(ResultSet rs) throws SQLException {
        Vocabulary vocabulary = new Vocabulary(
                rs.getInt("vocab_id"),
                rs.getString("name"),
                rs.getString("picture_url"),
                rs.getBoolean("is_shared"),
                rs.getInt("words_total")
        );
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            vocabulary.setCreateTime(createTime.toLocalDateTime());
        }
        return vocabulary;
    }

}