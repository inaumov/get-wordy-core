package get.wordy.core.api;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassService {

    List<ClassInfo> getClassesInfo(OwnerId ownerId);

    ClassInfo saveClassInfo(OwnerId ownerId, ClassInfo classInfo);

    boolean deleteClassInfo(OwnerId ownerId, String classId);

    ClassInfo findClassInfo(OwnerId ownerId, String classId);

}
