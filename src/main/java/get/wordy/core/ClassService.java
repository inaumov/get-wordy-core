package get.wordy.core;

import get.wordy.core.api.IClassService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.exception.DictionaryNotFoundException;
import get.wordy.core.api.exception.DictionaryServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.*;
import get.wordy.core.db.LocalTxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Supplier;

public class ClassService implements IClassService {

    private static final Logger LOG = LoggerFactory.getLogger(ClassService.class);

    private ClassesDao classesDao;
    private WordsheetDao wordsheetDao;
    private WordDao wordDao;
    private LocalTxManager connection;

    private final Map<OwnerId, List<ClassDetails>> classListCache = new HashMap<>();
    private final Map<String, List<WordsheetHeader>> classWordsheetCache = new HashMap<>();
    private final Map<Integer, List<Word>> wordsheetItemsCache = new HashMap<>();

    @SuppressWarnings("unused")
    public ClassService() {
    }

    @SuppressWarnings("unused")
    public ClassService(ClassesDao classesDao,
                        WordDao wordDao,
                        WordsheetDao wordsheetDao,
                        LocalTxManager connection
    ) {
        this.classesDao = classesDao;
        this.wordDao = wordDao;
        this.wordsheetDao = wordsheetDao;
        this.connection = connection;
    }

    @Override
    public List<ClassDetails> getClasses(OwnerId userId, String dayOfWeek) {
        List<ClassDetails> list;
        try {
            connection.open();
            // todo filter by day (a class can be assigned to several days)
            list = classesDao.selectAllByOwnerId(userId);
            connection.commit();
            classListCache.remove(userId);
            classListCache.put(userId, list);
        } catch (DaoException e) {
            LOG.error("Error while getting list of classes", e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return list; // todo copy from cache
    }

    @Override
    public ClassDetails saveClass(OwnerId userId, ClassDetails classDetails) {
        try {
            connection.open();
            ClassDetails inserted = classesDao.insert(userId, classDetails);
            connection.commit();
            putClassToCache(userId, () -> classDetails);
            return inserted;
        } catch (DaoException e) {
            LOG.error("Error while creating a new dictionary", e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean deleteClass(OwnerId userId, String classId) {
        // todo
        // todo check user owns class before delete
        classesDao.delete(userId, classId);
        return true;
    }

    @Override
    public List<WordsheetHeader> getWordsheetList(OwnerId userId, String classId) {
        // todo check user owns class
        List<WordsheetHeader> list;
        try {
            connection.open();
            list = wordsheetDao.selectAllByClassId(classId);
            connection.commit();
            classWordsheetCache.remove(classId);
            classWordsheetCache.put(classId, list);
        } catch (DaoException e) {
            LOG.error("Error while getting list of classes", e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return list; // todo copy from cache
    }

    @Override
    public WordsheetHeader createWordsheet(OwnerId userId, String classId, String name) {
        // todo check user has class
        try {
            connection.open();
            WordsheetHeader newWordsheet = wordsheetDao.insert(classId, name);
            connection.commit();
            putWordsheetToCache(classId, () -> newWordsheet);
            return newWordsheet;
        } catch (DaoException e) {
            LOG.error("Error while creating a new wordsheet for the class id = {}", classId, e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean renameWordsheet(OwnerId ownerId, String classId, int wordsheetId, String name) {
        try {
            WordsheetHeader wordsheet = findWordsheet(classId, wordsheetId);
            if (Objects.equals(wordsheet.name(), name)) {
                return true;
            }
            connection.open();
            WordsheetHeader renamed = wordsheetDao.rename(wordsheetId, name);
            connection.commit();
            classWordsheetCache.get(classId)
                    .add(renamed);
        } catch (DaoException e) {
            LOG.error("Error while renaming wordsheet, id = {}", wordsheetId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;

    }

    @Override
    public boolean makeWordsheetIsShared(OwnerId ownerId, String classId, int wordsheetId, boolean isShared) {
        try {
            WordsheetHeader wordsheet = findWordsheet(classId, wordsheetId);
            if (Objects.equals(wordsheet.isShared(), isShared)) {
                return true;
            }
            connection.open();
            WordsheetHeader updated = wordsheetDao.setIsShared(wordsheetId, isShared);
            connection.commit();
            classWordsheetCache.get(classId)
                    .add(updated);
        } catch (DaoException e) {
            LOG.error("Error while updating wordsheet, id = {}", wordsheetId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean deleteWordsheet(OwnerId ownerId, String classId, int wordsheetId) {
        // todo check if exists
        int rowsAffected = wordsheetDao.deleteWordsheetById(wordsheetId);
        if (rowsAffected == 0) {
            LOG.warn("Attempt to delete a wordsheet with id {} which does not exist.", wordsheetId);
            return false;
        }
        return true;
    }

    @Override
    public List<Word> getWords(OwnerId ownerId, String classId, int wordsheetId) {
        if (wordsheetItemsCache.containsKey(wordsheetId)) {
            List<Word> wordsheetItems = wordsheetItemsCache.get(wordsheetId);
            if (!CollectionUtils.isEmpty(wordsheetItems)) {
                return wordsheetItems;
            }
        }
        List<Word> wordsheet;
        try {
            connection.open();
            Set<Integer> wordsRefs = wordsheetDao.getWordsRefs(findWordsheet(classId, wordsheetId).wordsheetId());
            wordsheet = wordDao.selectAll(wordsRefs);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all words for wordsheet id = {}", wordsheetId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }

        wordsheetItemsCache.put(wordsheetId, wordsheet);

        return List.copyOf(wordsheet);
    }

    @Override
    public Word addToWordsheet(OwnerId ownerId, String classId, int wordsheetId, int wordId) {

        Word word = new Word();
        word = word.withId(wordId);
        List<Word> wordsheetItems = wordsheetItemsCache.get(wordsheetId);
        wordsheetItems.add(word);

        wordsheetDao.addToWordsheet(wordsheetId, Set.of(wordId));

        return word;
    }

    @Override
    public boolean removeFromWordsheet(OwnerId classOwnerId, String classId, int wordsheetId, int wordId) {

        List<Word> wordsheetItems = wordsheetItemsCache.get(wordsheetId);
        wordsheetItems.removeIf(wordsheetItem -> wordsheetItem.getId() == wordId);

        wordsheetDao.removeFromWordsheet(wordsheetId, Set.of(wordId));

        return true;
    }

    private void putClassToCache(OwnerId ownerId, Supplier<ClassDetails> classDetailsSupplier) {
        if (classListCache.containsKey(ownerId)) {
            List<ClassDetails> dictionaries = classListCache.get(ownerId);
            dictionaries.add(classDetailsSupplier.get());
        } else {
            List<ClassDetails> newList = new ArrayList<>();
            newList.add(classDetailsSupplier.get());
            classListCache.put(ownerId, newList);
        }
    }

    private void putWordsheetToCache(String classId, Supplier<WordsheetHeader> wordsheetHeaderSupplier) {
        if (classWordsheetCache.containsKey(classId)) {
            List<WordsheetHeader> wordsheetList = classWordsheetCache.get(classId);
            wordsheetList.add(wordsheetHeaderSupplier.get());
        } else {
            List<WordsheetHeader> newList = new ArrayList<>();
            newList.add(wordsheetHeaderSupplier.get());
            classWordsheetCache.put(classId, newList);
        }
    }

    private WordsheetHeader findWordsheet(String classId, int wordsheetId) {
        return classWordsheetCache.get(classId)
                .stream()
                .filter(wordsheetHeader -> wordsheetHeader.wordsheetId() == wordsheetId)
                .findAny()
                .orElseGet(() -> getWordsheetFromDb(classId, wordsheetId));
    }

    private WordsheetHeader getWordsheetFromDb(String classId, int wordsheetId) {
        WordsheetHeader wordsheet;
        try {
            connection.open();
            wordsheet = wordsheetDao.selectById(wordsheetId);
            connection.commit();
        } catch (DaoException e) {
            throw new DictionaryServiceException();
        }
        if (wordsheet == null) {
            throw new DictionaryNotFoundException();
        }
        putWordsheetToCache(classId, () -> wordsheet);
        return wordsheet;
    }

}
