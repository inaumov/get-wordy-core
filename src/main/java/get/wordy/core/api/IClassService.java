package get.wordy.core.api;

import get.wordy.core.api.bean.ClassInfo;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.bean.VocabHeader;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassService {

    List<ClassInfo> getClasses(OwnerId ownerId, String dayOfWeek);

    ClassInfo saveClass(OwnerId ownerId, ClassInfo classInfo);

    boolean deleteClass(OwnerId ownerId, String classId);

    List<VocabHeader> getVocabularies(OwnerId ownerId, String classId);

    VocabHeader createVocabulary(OwnerId ownerId, String classId, String name);

    boolean renameVocabulary(OwnerId ownerId, String classId, int vocabId, String name);

    boolean makeVocabularyIsShared(OwnerId ownerId, String classId, int vocabId, boolean isShared);

    boolean deleteVocabulary(OwnerId ownerId, String classId, int vocabId);

    List<Word> getWords(OwnerId ownerId, String classId, int vocabId);

    Word addToVocabulary(OwnerId ownerId, String classId, int vocabId, int wordId);

    boolean removeFromVocabulary(OwnerId ownerId, String classId, int vocabId, int wordId);

}
