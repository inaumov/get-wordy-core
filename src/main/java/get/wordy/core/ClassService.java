//package get.wordy.core;
//
//import get.wordy.core.api.IClassService;
//import get.wordy.core.api.bean.*;
//import get.wordy.core.api.exception.ClassInfoNotFoundException;
//import get.wordy.core.api.exception.ClassServiceException;
//import get.wordy.core.api.exception.WordsheetNotFoundException;
//import get.wordy.core.api.id.OwnerId;
//import get.wordy.core.dao.exception.DaoException;
//import get.wordy.core.dao.impl.*;
//import get.wordy.core.db.LocalTxManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.util.CollectionUtils;
//
//import java.util.*;
//import java.util.function.Supplier;
//
//public class ClassService implements IClassService {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ClassService.class);
//
//    private ClassesDao classesDao;
//    private VocabularyDao vocabularyDao;
//    private WordDao wordDao;
//    private LocalTxManager connection;
//
//    private final Map<OwnerId, List<ClassInfo>> classesListCache = new HashMap<>();
//    private final Map<String, List<VocabHeader>> classVocabulariesCache = new HashMap<>();
//    private final Map<Integer, List<Word>> wordsExplanationsCache = new HashMap<>();
//
//    @SuppressWarnings("unused")
//    public ClassService() {
//    }
//
//    @SuppressWarnings("unused")
//    public ClassService(ClassesDao classesDao,
//                        WordDao wordDao,
//                        VocabularyDao vocabularyDao,
//                        LocalTxManager connection
//    ) {
//        this.classesDao = classesDao;
//        this.wordDao = wordDao;
//        this.vocabularyDao = vocabularyDao;
//        this.connection = connection;
//    }
//
//    @Override
//    public List<ClassInfo> getClasses(OwnerId userId, String dayOfWeek) {
//
//        List<ClassInfo> cachedClassInfos = classesListCache.get(userId);
//        if (cachedClassInfos != null && !cachedClassInfos.isEmpty()) {
//            return cachedClassInfos;
//        }
//
//        List<ClassInfo> list;
//        try {
//            connection.open();
//            // fetch from the database if not present in the cache
//            // todo filter by day (a class can be assigned to several days)
//            list = classesDao.selectAllByOwnerId(userId);
//            connection.commit();
//
//            // update the cache
//            classesListCache.put(userId, list);
//        } catch (DaoException e) {
//            LOG.error("Error while getting list of class infos", e);
//            return Collections.emptyList();
//        } finally {
//            connection.close();
//        }
//        return list;
//    }
//
//    @Override
//    public ClassInfo saveClass(OwnerId userId, ClassInfo classInfo) {
//        try {
//            connection.open();
//            ClassInfo inserted = classesDao.insert(userId, classInfo);
//            connection.commit();
//            putClassInfoToCache(userId, () -> classInfo);
//            return inserted;
//        } catch (DaoException e) {
//            LOG.error("Error while creating a new dictionary", e);
//            connection.rollback();
//            return null;
//        } finally {
//            connection.close();
//        }
//    }
//
//    @Override
//    public boolean deleteClass(OwnerId userId, String classId) {
//        try {
//            ClassInfo classInfo = findClassInfo(userId, classId);
//            connection.open();
//            classesDao.delete(userId, classId);
//            connection.commit();
//            classesListCache.get(userId).remove(classInfo);
//        } catch (DaoException e) {
//            connection.rollback();
//            LOG.error("Error while removing class by id = {}", classId, e);
//            return false;
//        } finally {
//            connection.close();
//        }
//        return true;
//    }
//
//    @Override
//    public List<VocabHeader> getVocabularies(OwnerId userId, String classId) {
//        // todo check user owns class
//        List<VocabHeader> list;
//        try {
//            connection.open();
//            list = vocabularyDao.selectAllByClassId(classId);
//            connection.commit();
//            classVocabulariesCache.remove(classId);
//            classVocabulariesCache.put(classId, list);
//        } catch (DaoException e) {
//            LOG.error("Error while getting list of classes", e);
//            return Collections.emptyList();
//        } finally {
//            connection.close();
//        }
//        return list; // todo copy from cache
//    }
//
//    @Override
//    public VocabHeader createVocabulary(OwnerId userId, String classId, String name) {
//        // todo check user has class
//        try {
//            connection.open();
//            VocabHeader newWordsheet = vocabularyDao.insert(classId, name);
//            connection.commit();
//            putWordsheetToCache(classId, () -> newWordsheet);
//            return newWordsheet;
//        } catch (DaoException e) {
//            LOG.error("Error while creating a new vocabulary for the class id = {}", classId, e);
//            connection.rollback();
//            return null;
//        } finally {
//            connection.close();
//        }
//    }
//
//    @Override
//    public boolean renameVocabulary(OwnerId ownerId, String classId, int vocabId, String name) {
//        try {
//            VocabHeader vocabulary = findWordsheet(classId, vocabId);
//            if (Objects.equals(vocabulary.name(), name)) {
//                return true;
//            }
//            connection.open();
//            VocabHeader renamed = vocabularyDao.rename(vocabId, name);
//            connection.commit();
//            classVocabulariesCache.get(classId)
//                    .add(renamed);
//        } catch (DaoException e) {
//            LOG.error("Error while renaming vocabulary, id = {}", vocabId, e);
//            connection.rollback();
//            return false;
//        } finally {
//            connection.close();
//        }
//        return true;
//
//    }
//
//    @Override
//    public boolean makeVocabularyIsShared(OwnerId ownerId, String classId, int vocabId, boolean isShared) {
//        try {
//            VocabHeader vocabulary = findWordsheet(classId, vocabId);
//            if (Objects.equals(vocabulary.isShared(), isShared)) {
//                return true;
//            }
//            connection.open();
//            VocabHeader updated = vocabularyDao.setIsShared(vocabId, isShared);
//            connection.commit();
//            classVocabulariesCache.get(classId)
//                    .add(updated);
//        } catch (DaoException e) {
//            LOG.error("Error while updating vocabulary, id = {}", vocabId, e);
//            connection.rollback();
//            return false;
//        } finally {
//            connection.close();
//        }
//        return true;
//    }
//
//    @Override
//    public boolean deleteVocabulary(OwnerId ownerId, String classId, int vocabId) {
//        // todo check if exists
//        int rowsAffected = vocabularyDao.deleteVocabularyById(vocabId);
//        if (rowsAffected == 0) {
//            LOG.warn("Attempt to delete a vocabulary with id {} which does not exist.", vocabId);
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public List<Word> getWords(OwnerId ownerId, String classId, int vocabId) {
//        if (wordsExplanationsCache.containsKey(vocabId)) {
//            List<Word> wordsheetItems = wordsExplanationsCache.get(vocabId);
//            if (!CollectionUtils.isEmpty(wordsheetItems)) {
//                return wordsheetItems;
//            }
//        }
//        List<Word> vocabulary;
//        try {
//            connection.open();
//            Set<Integer> wordsRefs = vocabularyDao.getWordsRefs(findWordsheet(classId, vocabId).vocabId());
//            vocabulary = wordDao.selectAll(wordsRefs);
//            connection.commit();
//        } catch (DaoException e) {
//            LOG.error("Error while loading all words for vocabulary id = {}", vocabId, e);
//            return Collections.emptyList();
//        } finally {
//            connection.close();
//        }
//
//        wordsExplanationsCache.put(vocabId, vocabulary);
//
//        return List.copyOf(vocabulary);
//    }
//
//    @Override
//    public Word addToVocabulary(OwnerId ownerId, String classId, int vocabId, int wordId) {
//
//        Word word = new Word();
//        word = word.withId(wordId);
//        List<Word> wordsheetItems = wordsExplanationsCache.get(vocabId);
//        wordsheetItems.add(word);
//
//        vocabularyDao.addToVocabulary(vocabId, Set.of(wordId));
//
//        return word;
//    }
//
//    @Override
//    public boolean removeFromVocabulary(OwnerId classOwnerId, String classId, int vocabId, int wordId) {
//
//        List<Word> wordsheetItems = wordsExplanationsCache.get(vocabId);
//        wordsheetItems.removeIf(wordsheetItem -> wordsheetItem.getId() == wordId);
//
//        vocabularyDao.removeFromVocabulary(vocabId, Set.of(wordId));
//
//        return true;
//    }
//
//    private ClassInfo findClassInfo(OwnerId ownerId, String classId) {
//        return classesListCache.getOrDefault(ownerId, Collections.emptyList()).stream()
//                .filter(classInfo -> Objects.equals(classInfo.getClassId(), classId))
//                .findAny()
//                .orElseGet(() -> getClassInfoFromDb(ownerId, classId));
//    }
//
//    private ClassInfo getClassInfoFromDb(OwnerId ownerId, String classId) {
//        Optional<ClassInfo> classInfo;
//        try {
//            connection.open();
//            classInfo = classesDao.selectById(ownerId, classId);
//            connection.commit();
//        } catch (Exception e) {
//            throw new ClassServiceException("Could not get class info with id = " + classId + " for owner " + ownerId, e);
//        }
//        if (classInfo.isEmpty()) {
//            throw new ClassInfoNotFoundException("Class with id = " + classId + " not found for owner " + ownerId);
//        }
//        putClassInfoToCache(ownerId, classInfo::get);
//        return classInfo.get();
//    }
//
//    private void putClassInfoToCache(OwnerId ownerId, Supplier<ClassInfo> classDetailsSupplier) {
//        if (classesListCache.containsKey(ownerId)) {
//            List<ClassInfo> dictionaries = classesListCache.get(ownerId);
//            dictionaries.add(classDetailsSupplier.get());
//        } else {
//            List<ClassInfo> newList = new ArrayList<>();
//            newList.add(classDetailsSupplier.get());
//            classesListCache.put(ownerId, newList);
//        }
//    }
//
//    private void putWordsheetToCache(String classId, Supplier<VocabHeader> wordsheetHeaderSupplier) {
//        if (classVocabulariesCache.containsKey(classId)) {
//            List<VocabHeader> wordsheetList = classVocabulariesCache.get(classId);
//            wordsheetList.add(wordsheetHeaderSupplier.get());
//        } else {
//            List<VocabHeader> newList = new ArrayList<>();
//            newList.add(wordsheetHeaderSupplier.get());
//            classVocabulariesCache.put(classId, newList);
//        }
//    }
//
//    private VocabHeader findWordsheet(String classId, int vocabId) {
//        return classVocabulariesCache.get(classId)
//                .stream()
//                .filter(vocabHeader -> vocabHeader.vocabId() == vocabId)
//                .findAny()
//                .orElseGet(() -> getWordsheetFromDb(classId, vocabId));
//    }
//
//    private VocabHeader getWordsheetFromDb(String classId, int vocabId) {
//        Optional<VocabHeader> vocabulary;
//        try {
//            connection.open();
//            vocabulary = vocabularyDao.selectById(vocabId);
//            connection.commit();
//        } catch (Exception e) {
//            throw new ClassServiceException("Could not load vocabulary with id = " + vocabId + " for class id = " + classId, e);
//        }
//        if (vocabulary.isEmpty()) {
//            throw new WordsheetNotFoundException("Vocabulary with id = " + vocabId + " not found for class id = " + classId);
//        }
//        putWordsheetToCache(classId, vocabulary::get);
//        return vocabulary.get();
//    }
//
//}
