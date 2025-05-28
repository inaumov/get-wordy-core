package get.wordy.core.api;

import get.wordy.core.api.bean.ClassViewerInfo;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassAccessService {

    void assignUserToClass(OwnerId adminId, String classId, String targetUserId);

    void removeUserFromClass(OwnerId adminId, String classId, String targetUserId);

    List<ClassViewerInfo> getParticipantClasses(String userId);

    List<String> getAssignedParticipants(String classId);

    boolean hasAccess(String classId, String targetUserId);

    void activate(OwnerId adminId, String classId);

    void deactivate(OwnerId adminId, String classId);

}
