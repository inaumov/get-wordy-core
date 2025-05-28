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
import org.springframework.dao.DataAccessException;

import java.util.*;
import java.util.function.Supplier;

public class ClassService implements IClassService {

    private static final Logger LOG = LoggerFactory.getLogger(ClassService.class);

    private ClassesDao classesDao;
    private LocalTxManager connection;

    private final Map<OwnerId, Map<String, ClassInfo>> ownerToClassesCache = new HashMap<>();

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
    public List<ClassInfo> getClassesInfo(OwnerId ownerId) {

        Map<String, ClassInfo> cachedClassInfos = ownerToClassesCache.get(ownerId);
        if (cachedClassInfos != null && !cachedClassInfos.isEmpty()) {
            return List.copyOf(cachedClassInfos.values());
        }

        // fetch from the database if not present in the cache
        Map<String, ClassInfo> classIdIndex;
        try {
            classIdIndex = classesDao.fetchAllClasses(ownerId);
            // update the cache
            ownerToClassesCache.put(ownerId, classIdIndex);
        } catch (DataAccessException e) {
            LOG.error("Error while getting classes info for the owner = {}", ownerId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return List.copyOf(classIdIndex.values());
    }

    @Override
    public List<ClassInfo> getClassesInfo(Set<String> classIds) {
        List<ClassInfo> classInfos;
        try {
            classInfos = classesDao.selectByIds(classIds);
        } catch (DataAccessException e) {
            LOG.error("Error while getting classes info by ids = {}", classIds, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return List.copyOf(classInfos);
    }

    @Override
    public ClassInfo saveClassInfo(OwnerId ownerId, ClassInfo classInfo) {
        try {
            connection.open();
            ClassInfo inserted = classesDao.insert(ownerId, classInfo);
            connection.commit();
            putClassInfoToCache(ownerId, () -> classInfo);
            return inserted;
        } catch (DaoException e) {
            LOG.error("Error while saving a new class info for the owner = {}", ownerId, e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public void deleteClassInfo(OwnerId ownerId, String classId) {
        ClassInfo classInfo = findClassInfo(ownerId, classId);
        try {
            connection.open();
            classesDao.delete(ownerId, classId);
            connection.commit();
            ownerToClassesCache.remove(ownerId);
        } catch (DaoException e) {
            connection.rollback();
            LOG.error("Error while removing class info by id = {}, for the owner = {}", classId, ownerId, e);
        } finally {
            connection.close();
        }
    }

    @Override
    public void updateActivation(OwnerId ownerId, String classId, boolean isActive) {
        ClassInfo classInfo = findClassInfo(ownerId, classId);
        try {
            connection.open();
            classesDao.updateActivation(ownerId, classId, isActive);
            if (Boolean.FALSE.equals(isActive) && !classInfo.getTimeSlots().isEmpty()) {
                classesDao.resetSchedule(ownerId, classId);
            }
            connection.commit();
            ownerToClassesCache.remove(ownerId);
        } catch (DaoException e) {
            connection.rollback();
            LOG.error("Error while updating class activation by id = {} for the owner = {}", classId, ownerId, e);
        } finally {
            connection.close();
        }
    }

    @Override
    public ClassInfo findClassInfo(OwnerId ownerId, String classId) {
        Map<String, ClassInfo> userClasses = ownerToClassesCache.getOrDefault(ownerId, Collections.emptyMap());
        return findClassById(userClasses, classId)
                .orElseGet(() -> getClassInfoFromDb(ownerId, classId));
    }

    private ClassInfo getClassInfoFromDb(OwnerId ownerId, String classId) {
        Optional<ClassInfo> classInfo;
        try {
            classInfo = classesDao.selectById(ownerId, classId);
        } catch (Exception e) {
            throw new ClassServiceException("Could not get class info with id = " + classId + " for owner " + ownerId, e);
        }
        if (classInfo.isEmpty()) {
            throw new ClassInfoNotFoundException("Class info with id = " + classId + " not found for owner " + ownerId);
        }
        putClassInfoToCache(ownerId, classInfo::get);
        return classInfo.get();
    }

    private void putClassInfoToCache(OwnerId ownerId, Supplier<ClassInfo> classInfoSupplier) {
        ClassInfo classInfo = classInfoSupplier.get();
        if (ownerToClassesCache.containsKey(ownerId)) {
            Map<String, ClassInfo> classIdIndex = ownerToClassesCache.get(ownerId);
            classIdIndex.put(classInfo.getClassId(), classInfo);
        } else {
            Map<String, ClassInfo> classIdIndex = new HashMap<>();
            classIdIndex.put(classInfo.getClassId(), classInfo);
            ownerToClassesCache.put(ownerId, classIdIndex);
        }
    }

    // efficiently find a class by classId from the reverse index
    private Optional<ClassInfo> findClassById(Map<String, ClassInfo> classIdIndex, String classId) {
        return Optional.ofNullable(classIdIndex.get(classId));
    }

}
