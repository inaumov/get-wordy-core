package get.wordy.core.api;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassService {

    List<ClassInfo> getClasses(OwnerId ownerId, String dayOfWeek);

    ClassInfo saveClass(OwnerId ownerId, ClassInfo classInfo);

    boolean deleteClass(OwnerId ownerId, String classId);

}
