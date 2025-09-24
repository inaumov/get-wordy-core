package get.wordy.core;

import get.wordy.core.api.IWordExplanationService;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.exception.WordNotFoundException;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WordsExplanationService implements IWordExplanationService {

    private static final Logger LOG = LoggerFactory.getLogger(WordsExplanationService.class);

    private WordDao wordDao;
    private LocalTxManager connection;

    private final Map<Integer, Word> wordsCache = new HashMap<>();

    @SuppressWarnings("unused")
    public WordsExplanationService() {
    }

    @SuppressWarnings("unused")
    public WordsExplanationService(WordDao wordDao,
                                   LocalTxManager connection
    ) {
        this.wordDao = wordDao;
        this.connection = connection;
    }

    @Override
    public List<Word> findExplanations(String value) {
        return loadFullWordHeadlinesFromDb(value);
    }

    @Override
    public Word getWordExplanation(int wordId) {
        return Optional.ofNullable(wordsCache.get(wordId))
                .orElseGet(() -> loadFullWordHeadlineFromDb(wordId));
    }

    @Override
    public Word addWordExplanation(Word entity) {
        try {
            connection.open();
            Word word = wordDao.insert(entity);
            connection.commit();
            if (word.getId() > 0) {
                wordsCache.put(word.getId(), word);
                return word;
            } else {
                throw new DictionaryServiceException();
            }
        } catch (DaoException e) {
            LOG.error("Error while saving a new word", e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
    }

    @Override
    public Word updateWordExplanation(Word entity) {
        return null;
    }

    @Override
    public boolean deleteWordExplanationPermanently(int wordId) {
        try {
            connection.open();
            wordDao.delete(wordId);
            connection.commit();
            wordsCache.remove(wordId);
        } catch (DaoException e) {
            LOG.error("Error while removing word by id = {}", wordId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    private Word loadFullWordHeadlineFromDb(int wordId) {
        Word word;
        try {
            word = wordDao.findById(wordId);
        } catch (DaoException e) {
            LOG.error("Error while loading a word headline by id = {}", wordId, e);
            throw new DictionaryServiceException();
        }
        if (word == null) {
            throw new WordNotFoundException();
        }
        wordsCache.put(wordId, word);
        return word;
    }

    private List<Word> loadFullWordHeadlinesFromDb(String lemma) {
        List<Word> words;
        try {
            connection.open();
            words = wordDao.findByLemma(lemma);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading a word by lemma = {}", lemma, e);
            throw new DictionaryServiceException();
        }
        for (Word word : words) {
            wordsCache.put(word.getId(), word);
        }
        return words;
    }

}
