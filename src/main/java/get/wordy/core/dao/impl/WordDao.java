package get.wordy.core.dao.impl;

import get.wordy.core.api.bean.Sentence;
import get.wordy.core.api.bean.Word;
import get.wordy.core.dao.exception.DaoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class WordDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WordDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String INSERT_QUERY = """
            INSERT INTO words (lemma, part_of_speech, transcription, meaning, register, domain, level)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
    private static final String DELETE_QUERY = "DELETE FROM words WHERE id = ?";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM words WHERE id = ?";
    private static final String FIND_ALL_QUERY = "SELECT * FROM words WHERE id IN (%s)";

    /**
     * This gives you a precise ordering contract:
     * Highest similarity first
     * If similarity is equal → shorter lemma first
     * If still equal → lower ID first
     */
    private static final String FUZZY_SEARCH_QUERY = """
            SELECT *,
                   similarity(lemma, ?) AS score
            FROM words
            WHERE similarity(lemma, ?) >= ?
            ORDER BY
                score DESC,
                length(lemma) ,
                id
            LIMIT 5;
            """;
    private static final String UPDATE_QUERY = """
            UPDATE words
            SET lemma = ?, part_of_speech = ?, transcription = ?, meaning = ?, register = ?, domain = ?, level = ?
            WHERE id = ?
            """;

    // sentences and collocations
    private static final String INSERT_SENTENCE_QUERY =
            "INSERT INTO in_context (word_id, example, matched_words) VALUES (?,?,?)";
    private static final String INSERT_COLLOCATIONS_QUERY =
            "INSERT INTO collocations (word_id, phrase) VALUES (?,?)";
    private static final String SELECT_FROM_CONTEXT_QUERY =
            "SELECT * FROM in_context WHERE word_id=?";
    private static final String SELECT_COLLOCATIONS_QUERY =
            "SELECT * FROM collocations WHERE word_id=?";
    private static final String DELETE_FROM_CONTEXT_QUERY =
            "DELETE FROM in_context WHERE word_id=?";
    private static final String DELETE_COLLOCATIONS_QUERY =
            "DELETE FROM collocations WHERE word_id=?";

    public Word insert(Word word) {
        Integer id = jdbcTemplate.queryForObject(INSERT_QUERY, Integer.class,
                word.getLemma(),
                word.getPartOfSpeech(),
                word.getTranscription(),
                word.getMeaning(),
                word.getRegister(),
                word.getDomain(),
                word.getLevel()
        );

        insertSentences(id, word.getSentences());
        insertCollocations(id, word.getCollocations());
        return word.withId(id);
    }

    public List<Word> addWords(Collection<Word> words) {
        if (words.isEmpty()) {
            return List.of();
        }

        // Build bulk INSERT with RETURNING
        String sql = """
                INSERT INTO words (lemma, part_of_speech, transcription, meaning, register, domain, level)
                VALUES %s
                RETURNING id
                """;

        String placeholders = words.stream()
                .map(l -> "(?, ?, ?, ?, ?, ?, ?)")
                .collect(Collectors.joining(", "));
        sql = sql.formatted(placeholders);

        Object[] params = words.stream()
                .flatMap(word -> Stream.of(
                        word.getLemma(),
                        word.getPartOfSpeech(),
                        word.getTranscription(),
                        word.getMeaning(),
                        word.getRegister(),
                        word.getDomain(),
                        word.getLevel()
                ))
                .toArray();

        List<Integer> ids = jdbcTemplate.query(sql, rs -> {
            List<Integer> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getInt("id"));
            }
            return result;
        }, params);

        if (ids == null || words.size() != ids.size()) {
            throw new IllegalArgumentException("Words and IDs must have the same size");
        }

        List<Word> withIds = new ArrayList<>();
        Iterator<Integer> idIterator = ids.iterator();
        for (Word lemma : words) {
            if (idIterator.hasNext()) {
                withIds.add(lemma.withId(idIterator.next()));
            }
        }

        insertAllSentencesInBatch(withIds);
        insertAllCollocationsInBatch(withIds);

        return withIds;
    }

    public void delete(int wordId) throws DaoException {
        try {
            jdbcTemplate.update(DELETE_QUERY, wordId);
        } catch (DataIntegrityViolationException ex) {
            throw new DaoException("Error while deleting a word record", ex);
        }
    }

    public int update(Word word) {
        int updated = jdbcTemplate.update(UPDATE_QUERY,
                word.getLemma(),
                word.getPartOfSpeech(),
                word.getTranscription(),
                word.getMeaning(),
                word.getRegister(),
                word.getDomain(),
                word.getLemma(),
                word.getId()
        );

        deleteFromContext(word.getId());
        deleteFromCollocations(word.getId());
        insertSentences(word.getId(), word.getSentences());
        insertCollocations(word.getId(), word.getCollocations());
        return updated;
    }

    public Word findById(int id) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, this::mapRowToObject, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Word> findAllByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of(); // safer than throwing
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format(FIND_ALL_QUERY, placeholders);
        return jdbcTemplate.query(sql, this::mapRowToObject, ids.toArray());
    }

    /*
    Returns matching words ordered by descending fuzzy similarity.
    The first word is the closest match.
    Returns an empty list if no word exceeds the similarity threshold.
     */
    public List<Word> findByLemma(String lemma) {
        double threshold = autoThreshold(lemma);
        return jdbcTemplate.query(FUZZY_SEARCH_QUERY, this::mapRowToObject, lemma, lemma, threshold);
    }

    private double autoThreshold(String lemma) {
        if (lemma == null || lemma.isBlank() || lemma.trim().length() < 2) {
            throw new IllegalArgumentException("lemma should contain at least 2 characters");
        }
        int length = lemma.length();
        return switch (length) {
            case 2, 3 -> 0.70;          // 2–3 characters   → very strict
            case 4, 5 -> 0.50;          // 4–5 characters   → strict
            case 6, 7, 8 -> 0.45;       // 6–8 characters   → moderate
            case 9, 10 -> 0.40;         // 9–10 characters  → permissive
            default -> 0.35;            // 11+ characters   → most permissive
        };
    }

    public List<Word> findExistingWords(Set<String> lemmas) {

        if (lemmas.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT lemma,
                       lower(part_of_speech) AS part_of_speech,
                       level,
                       id
                FROM words
                WHERE lower(lemma) = ANY(CAST(? AS text[]))
                """;

        List<Word> result = jdbcTemplate.query(
                sql,
                ps -> ps.setArray(
                        1,
                        ps.getConnection().createArrayOf(
                                "text",
                                lemmas.stream()
                                        .map(String::toLowerCase)
                                        .toArray()
                        )
                ),
                (rs, rowNum) -> new Word(
                        rs.getInt("id"),
                        rs.getString("lemma"),
                        rs.getString("part_of_speech"),
                        null,
                        null,
                        rs.getString("level")
                )
        );
        return new ArrayList<>(result);
    }

    private Word mapRowToObject(ResultSet rs, int rowNum) throws SQLException {
        Word word = new Word(
                rs.getInt("id"),
                rs.getString("lemma"),
                rs.getString("part_of_speech"),
                rs.getString("transcription"),
                rs.getString("meaning"),
                rs.getString("level")
        );
        word.setRegister(rs.getString("register"));
        word.setDomain(rs.getString("domain"));
        word.setSentences(getSentencesFor(word.getId()));
        word.setCollocations(getCollocationsFor(word.getId()));
        return word;
    }

    private List<Sentence> getSentencesFor(int wordId) {
        return jdbcTemplate.query(SELECT_FROM_CONTEXT_QUERY,
                (rs, rowNum) -> new Sentence(
                        rs.getString("example"),
                        rs.getString("matched_words")
                ),
                wordId);
    }

    private List<String> getCollocationsFor(int wordId) {
        return jdbcTemplate.query(SELECT_COLLOCATIONS_QUERY,
                (rs, rowNum) -> rs.getString("phrase"),
                wordId);
    }

    private void insertSentences(Integer wordId, List<Sentence> sentences) {
        if (sentences == null || sentences.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_SENTENCE_QUERY,
                sentences,
                sentences.size(),
                (ps, sentence) -> {
                    ps.setInt(1, wordId);
                    ps.setString(2, sentence.example());
                    ps.setString(3, sentence.matchedWords());
                });
    }

    private void insertCollocations(Integer wordId, List<String> collocations) {
        if (collocations == null || collocations.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_COLLOCATIONS_QUERY,
                collocations,
                collocations.size(),
                (ps, collocation) -> {
                    ps.setInt(1, wordId);
                    ps.setString(2, collocation);
                });
    }

    public void insertAllSentencesInBatch(List<Word> words) throws DaoException {
        if (words == null || words.isEmpty()) {
            return;
        }

        try {
            List<Object[]> params = words.stream()
                    .flatMap(word -> word.getSentences().stream()
                            .map(s -> new Object[]{word.getId(), s.example(), s.matchedWords()}))
                    .toList();

            jdbcTemplate.batchUpdate(INSERT_SENTENCE_QUERY, params);
        } catch (DataAccessException ex) {
            throw new DaoException("Error while inserting sentences", ex);
        }
    }

    public void insertAllCollocationsInBatch(List<Word> words) throws DaoException {
        if (words == null || words.isEmpty()) {
            return;
        }

        try {
            List<Object[]> params = words.stream()
                    .flatMap(word -> word.getCollocations().stream()
                            .map(c -> new Object[]{word.getId(), c}))
                    .toList();

            jdbcTemplate.batchUpdate(INSERT_COLLOCATIONS_QUERY, params);
        } catch (DataAccessException ex) {
            throw new DaoException("Error while inserting collocations", ex);
        }
    }

    private void deleteFromContext(int wordId) {
        jdbcTemplate.update(DELETE_FROM_CONTEXT_QUERY, wordId);
    }

    private void deleteFromCollocations(int wordId) {
        jdbcTemplate.update(DELETE_COLLOCATIONS_QUERY, wordId);
    }

}