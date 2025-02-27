package get.wordy.core.api;

import get.wordy.core.api.bean.ClassViewerInfo;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassAccessService {

    void assignUserToClass(OwnerId adminId, String classId, String targetUserId);

    void removeUserFromClass(OwnerId adminId, String classId, String targetUserId);

    List<ClassViewerInfo> getAttendeeClasses(String userId);

    List<String> getAssignedAttendees(String classId);
}
