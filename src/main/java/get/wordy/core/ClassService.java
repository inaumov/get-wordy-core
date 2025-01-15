package get.wordy.core;

import get.wordy.core.api.IClassService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.exception.ClassInfoNotFoundException;
import get.wordy.core.api.exception.ClassServiceException;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.*;
import get.wordy.core.db.LocalTxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

public class ClassService implements IClassService {

    private static final Logger LOG = LoggerFactory.getLogger(ClassService.class);

    private ClassesDao classesDao;
    private LocalTxManager connection;

    private final Map<OwnerId, List<ClassInfo>> classesListCache = new HashMap<>();

    @SuppressWarnings("unused")
    public ClassService() {
    }

    @SuppressWarnings("unused")
    public ClassService(ClassesDao classesDao,
                        LocalTxManager connection
    ) {
        this.classesDao = classesDao;
        this.connection = connection;
    }

    @Override
    public List<ClassInfo> getClasses(OwnerId userId, String dayOfWeek) {

        List<ClassInfo> cachedClassInfos = classesListCache.get(userId);
        if (cachedClassInfos != null && !cachedClassInfos.isEmpty()) {
            return cachedClassInfos;
        }

        List<ClassInfo> list;
        try {
            connection.open();
            // fetch from the database if not present in the cache
            // todo filter by day (a class can be assigned to several days)
            list = classesDao.selectAllByOwnerId(userId);
            connection.commit();

            // update the cache
            classesListCache.put(userId, list);
        } catch (DaoException e) {
            LOG.error("Error while getting list of class infos", e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return list;
    }

    @Override
    public ClassInfo saveClass(OwnerId userId, ClassInfo classInfo) {
        try {
            connection.open();
            ClassInfo inserted = classesDao.insert(userId, classInfo);
            connection.commit();
            putClassInfoToCache(userId, () -> classInfo);
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
        try {
            ClassInfo classInfo = findClassInfo(userId, classId);
            connection.open();
            classesDao.delete(userId, classId);
            connection.commit();
            classesListCache.get(userId).remove(classInfo);
        } catch (DaoException e) {
            connection.rollback();
            LOG.error("Error while removing class by id = {}", classId, e);
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    private ClassInfo findClassInfo(OwnerId ownerId, String classId) {
        return classesListCache.getOrDefault(ownerId, Collections.emptyList()).stream()
                .filter(classInfo -> Objects.equals(classInfo.getClassId(), classId))
                .findAny()
                .orElseGet(() -> getClassInfoFromDb(ownerId, classId));
    }

    private ClassInfo getClassInfoFromDb(OwnerId ownerId, String classId) {
        Optional<ClassInfo> classInfo;
        try {
            connection.open();
            classInfo = classesDao.selectById(ownerId, classId);
            connection.commit();
        } catch (Exception e) {
            throw new ClassServiceException("Could not get class info with id = " + classId + " for owner " + ownerId, e);
        }
        if (classInfo.isEmpty()) {
            throw new ClassInfoNotFoundException("Class with id = " + classId + " not found for owner " + ownerId);
        }
        putClassInfoToCache(ownerId, classInfo::get);
        return classInfo.get();
    }

    private void putClassInfoToCache(OwnerId ownerId, Supplier<ClassInfo> classDetailsSupplier) {
        if (classesListCache.containsKey(ownerId)) {
            List<ClassInfo> dictionaries = classesListCache.get(ownerId);
            dictionaries.add(classDetailsSupplier.get());
        } else {
            List<ClassInfo> newList = new ArrayList<>();
            newList.add(classDetailsSupplier.get());
            classesListCache.put(ownerId, newList);
        }
    }

}
