package get.wordy.core.api;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;

import java.util.List;
import java.util.Set;

public interface IClassService {

    List<ClassInfo> getClassesInfo(OwnerId ownerId);

    List<ClassInfo> getClassesInfo(Set<String> classIds);

    ClassInfo saveClassInfo(OwnerId ownerId, ClassInfo classInfo);

    boolean deleteClassInfo(OwnerId ownerId, String classId);

    void updateActivation(OwnerId ownerId, String classId, boolean isActive);

    ClassInfo findClassInfo(OwnerId ownerId, String classId);

}
