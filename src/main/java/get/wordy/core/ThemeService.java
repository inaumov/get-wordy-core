package get.wordy.core;

import get.wordy.core.api.bean.Theme;
import get.wordy.core.api.bean.ThemeStatus;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.bean.WordKey;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.exception.ThemeNotFoundException;
import get.wordy.core.api.exception.WordNotFoundException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.ThemeDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ThemeService {
    private static final Logger LOG = LoggerFactory.getLogger(ThemeService.class);

    private final ThemeDao themeDao;
    private final WordDao wordDao;
    private final LocalTxManager connection;

    private final Map<OwnerId, List<Theme>> themesCache = new HashMap<>();
    private final Map<Integer, List<Word>> wordsInThemeCache = new HashMap<>();

    public ThemeService(ThemeDao themeDao, WordDao wordDao, LocalTxManager connection) {
        this.themeDao = themeDao;
        this.wordDao = wordDao;
        this.connection = connection;
    }

    public List<Theme> getAllThemes(OwnerId ownerId) {
        List<Theme> cached = themesCache.get(ownerId);
        if (cached != null) {
            return List.copyOf(cached);
        }
        try {
            connection.open();
            List<Theme> themes = themeDao.findAll(ownerId);
            themesCache.put(ownerId, new ArrayList<>(themes));
            return List.copyOf(themes);
        } catch (DaoException e) {
            LOG.error("Error while getting themes for owner={}", ownerId, e);
            return List.of();
        } finally {
            connection.close();
        }
    }

    public Theme getTheme(OwnerId ownerId, int themeId) {
        List<Theme> cached = themesCache.get(ownerId);
        if (cached != null) {
            Theme theme = cached.stream()
                    .filter(t -> Objects.equals(t.themeId(), themeId))
                    .findFirst()
                    .orElse(null);
            if (theme != null) {
                return theme;
            }
        }
        try {
            connection.open();
            return themeDao.findById(ownerId, themeId)
                    .orElseThrow(() -> new ThemeNotFoundException("Theme with id = " + themeId + " not found for owner id = " + ownerId));
        } catch (DaoException e) {
            LOG.error("Error while getting theme={}, owner={}", themeId, ownerId, e);
            return null;
        } finally {
            connection.close();
        }
    }

    public Theme createTheme(OwnerId ownerId, String themeName) {
        try {
            connection.open();
            var created = themeDao.create(ownerId, themeName);
            themesCache.computeIfPresent(ownerId, (id, themes) -> {
                themes.add(created);
                themes.sort(Comparator.comparing(Theme::name));
                return themes;
            });
            connection.commit();
            return created;
        } catch (DaoException e) {
            LOG.error("Error while creating theme for owner={}", ownerId, e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    public Theme updateThemeName(OwnerId ownerId, int themeId, String name) {
        try {
            connection.open();
            var updated = themeDao.rename(ownerId, themeId, name);
            updateCache(ownerId, updated);
            connection.commit();
            return updated;
        } catch (DaoException e) {
            LOG.error("Error while updating theme={} name, owner={}", themeId, ownerId, e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    public Theme updateThemeStatus(OwnerId ownerId, int themeId, ThemeStatus status) {
        try {
            connection.open();
            var updated = themeDao.updateStatus(ownerId, themeId, status);
            updateCache(ownerId, updated);
            connection.commit();
            return updated;
        } catch (DaoException e) {
            LOG.error("Error while updating theme={} status, owner={}", themeId, ownerId, e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    public boolean deleteTheme(OwnerId ownerId, int themeId) {
        try {
            connection.open();
            boolean deleted = themeDao.delete(ownerId, themeId);
            themesCache.computeIfPresent(ownerId, (id, themes) -> {
                themes.removeIf(t -> Objects.equals(t.themeId(), themeId));
                return themes;
            });
            wordsInThemeCache.remove(themeId);
            connection.commit();
            return deleted;
        } catch (DaoException e) {
            connection.rollback();
            LOG.error("Error deleting theme={}", themeId, e);
            return false;
        } finally {
            connection.close();
        }
    }

    public List<Word> findExistingWords(Collection<WordKey> words) {
        try {
            Set<String> lemmas = words.stream()
                    .map(WordKey::lemma)
                    .collect(Collectors.toSet());

            return wordDao.findExistingWords(lemmas);
        } catch (DaoException e) {
            LOG.error("Error while finding words = {}", words, e);
            throw new DictionaryServiceException();
        }
    }

    public List<Word> getWords(OwnerId ownerId, int themeId) {
        List<Word> cached = wordsInThemeCache.get(themeId);
        if (cached != null) {
            return List.copyOf(cached);
        }
        try {
            connection.open();
            if (!themeDao.hasAccess(ownerId, themeId)) {
                throw new ThemeNotFoundException("Cannot load words because of no access");
            }
            Set<Integer> ids = themeDao.getWordIds(themeId);
            List<Word> words = wordDao.findAllByIds(ids);
            wordsInThemeCache.put(themeId, new ArrayList<>(words));
            return List.copyOf(words);
        } catch (DaoException e) {
            LOG.error("Error loading words for theme={}", themeId, e);
            return List.of();
        } finally {
            connection.close();
        }
    }

    public Word addWordToTheme(OwnerId ownerId, int themeId, int wordId) {
        try {
            connection.open();
            if (!themeDao.hasAccess(ownerId, themeId)) {
                throw new DictionaryServiceException("Cannot modify theme");
            }
            themeDao.addWordsToTheme(themeId, wordId);
            connection.commit();
            Word word = loadWordFromDb(wordId);
            invalidateThemeCache(ownerId, themeId);
            return word;
        } catch (DaoException e) {
            LOG.error("Error adding word={} theme={}", wordId, themeId, e);
            connection.rollback();
            throw new DictionaryServiceException();
        } finally {
            connection.close();
        }
    }

    public void removeWordFromTheme(OwnerId ownerId, int themeId, int wordId) {
        try {
            connection.open();
            themeDao.removeWordsFromTheme(themeId, wordId);
            connection.commit();
            invalidateThemeCache(ownerId, themeId);
        } catch (DaoException e) {
            LOG.error("Error removing word={} theme={}", wordId, themeId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    private Word loadWordFromDb(int wordId) {
        Word word;
        try {
            word = wordDao.findById(wordId);
        } catch (DaoException e) {
            LOG.error("Error while loading a word by id = {}", wordId, e);
            throw new DictionaryServiceException();
        }
        if (word == null) {
            throw new WordNotFoundException();
        }
        return word;
    }

    private void updateCache(OwnerId ownerId, Theme updated) {
        themesCache.computeIfPresent(ownerId, (id, themes) -> {
            themes.replaceAll(t ->
                    Objects.equals(t.themeId(), updated.themeId()) ? updated : t
            );
            themes.sort(Comparator.comparing(Theme::name));
            return themes;
        });
    }

    private void invalidateThemeCache(OwnerId ownerId, int themeId) {
        themesCache.remove(ownerId);
        wordsInThemeCache.remove(themeId);
    }

}
