package get.wordy.core;

import get.wordy.core.api.IClassAccessService;
import get.wordy.core.api.IClassService;
import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.bean.ClassViewerInfo;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.ClassAccessDao;
import get.wordy.core.db.LocalTxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

public class ClassAccessService implements IClassAccessService {

    private static final Logger LOG = LoggerFactory.getLogger(ClassAccessService.class);

    private ClassAccessDao classAccessDao;
    private LocalTxManager connection;
    private IClassService classService;
    private final Map<String, List<ClassViewerInfo>> userHasClassesCache = new HashMap<>();

    @SuppressWarnings("unused")
    public ClassAccessService() {
    }

    @SuppressWarnings("unused")
    public ClassAccessService(ClassAccessDao classAccessDao,
                              LocalTxManager connection,
                              IClassService classService
    ) {
        this.classAccessDao = classAccessDao;
        this.connection = connection;
        this.classService = classService;
    }

    @Override
    public void assignUserToClass(OwnerId adminId, String classId, String targetUserId) {
        try {
            connection.open();
            // ensure adminUserId has permission to manage access
            if (!classAccessDao.hasFullAccess(classId, adminId)) {
                throw new AccessDeniedException("Admin does not have permission to manage this class = " + classId);
            }
            classAccessDao.grantAccess(classId, targetUserId);
            connection.commit();
            LOG.info("User {} successfully assigned to class {}", targetUserId, classId);
        } catch (DaoException e) {
            LOG.error("Error while granting access to a class = {}, for the user = {}", classId, targetUserId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    @Override
    public void removeUserFromClass(OwnerId adminId, String classId, String targetUserId) {
        try {
            connection.open();
            // ensure adminUserId has permission to manage access
            if (!classAccessDao.hasFullAccess(classId, adminId)) {
                throw new AccessDeniedException("Admin does not have permission to manage this class = " + classId);
            }

            if (!classAccessDao.hasViewAccess(classId, targetUserId)) {
                throw new AccessDeniedException("No viewer has access to this class = " + classId);
            }
            classAccessDao.revokeAccess(classId, targetUserId);
            connection.commit();
            userHasClassesCache.remove(targetUserId);
            LOG.info("User {} successfully revoked from class {}", targetUserId, classId);
        } catch (DaoException e) {
            LOG.error("Error while revoking access to a class = {}, for user = {}", classId, targetUserId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    @Override
    public List<ClassViewerInfo> getAttendeeClasses(String userId) {
        List<ClassViewerInfo> userClasses = userHasClassesCache.get(userId);
        if (userClasses != null && !userClasses.isEmpty()) {
            return List.copyOf(userClasses);
        }
        // fetch from the database if not present in the cache
        Map<String, Boolean> accessibleClasses = classAccessDao.findAccessibleClasses(userId);
        List<ClassInfo> classesInfo = classService.getClassesInfo(accessibleClasses.keySet());
        List<ClassViewerInfo> classViewerInfos = classesInfo
                .stream()
                .map(classInfo -> {
                    Boolean isActive = accessibleClasses.get(classInfo.getClassId());
                    ClassViewerInfo classViewerInfo = new ClassViewerInfo(
                            classInfo.getClassId(),
                            isActive,
                            classInfo.getName(),
                            isActive ? classInfo.getNotes() : null,
                            classInfo.getIsRepeatable()
                    );
                    if (classInfo.getIsRepeatable()) {
                        classViewerInfo.setTimeSlots(classInfo.getTimeSlots());
                    } else {
                        classViewerInfo.setEndDate(classInfo.getEndDate());
                    }
                    return classViewerInfo;
                })
                .toList();
        // update the cache
        userHasClassesCache.put(userId, classViewerInfos);
        return classViewerInfos;
    }

    @Override
    public List<String> getAssignedAttendees(String classId) {
        return classAccessDao.findAssignedViewersByClassId(classId);
    }

    @Override
    public boolean hasAccess(String classId, String targetUserId) {
        if (!classAccessDao.hasViewAccess(classId, targetUserId)) {
            throw new AccessDeniedException("Target viewer = " + targetUserId + " has no access to this class = " + classId);
        }
        return true;
    }

    @Override
    public void activate(OwnerId adminId, String classId) {
        try {
            connection.open();
            // ensure adminUserId has permission to manage access
            if (!classAccessDao.hasFullAccess(classId, adminId)) {
                throw new AccessDeniedException("Admin does not have permission to active this class = " + classId);
            }
            if (hasViewers(classId)) {
                // avoid throwing not found exception
                classAccessDao.updateActivation(classId, true);
            }
            classService.updateActivation(adminId, classId, true);
            connection.commit();
            userHasClassesCache.clear();
            LOG.info("ClassId {} has been activated", classId);
        } catch (DaoException e) {
            LOG.error("Error while activating a class = {}", classId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    @Override
    public void deactivate(OwnerId adminId, String classId) {
        try {
            connection.open();
            // ensure adminUserId has permission to manage access
            if (!classAccessDao.hasFullAccess(classId, adminId)) {
                throw new AccessDeniedException("Admin does not have permission to deactivate this class = " + classId);
            }
            if (hasViewers(classId)) {
                // avoid throwing not found exception
                classAccessDao.updateActivation(classId, false);
            }
            classService.updateActivation(adminId, classId, false);
            connection.commit();
            userHasClassesCache.clear();
            LOG.info("ClassId {} has been deactivated", classId);
        } catch (DaoException e) {
            LOG.error("Error while deactivating a class = {}", classId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    private boolean hasViewers(String classId) {
        return !classAccessDao.findAssignedViewersByClassId(classId).isEmpty();
    }

}
