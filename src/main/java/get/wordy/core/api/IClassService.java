package get.wordy.core.api;

import get.wordy.core.api.bean.ClassDetails;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.bean.WordsheetItem;
import get.wordy.core.api.bean.WordsheetHeader;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IClassService {

    List<ClassDetails> getClasses(OwnerId ownerId, String dayOfWeek);

    ClassDetails saveClass(OwnerId ownerId, ClassDetails classDetails);

    boolean deleteClass(OwnerId ownerId, String classId);

    List<WordsheetHeader> getWordsheetList(OwnerId ownerId, String classId);

    WordsheetHeader createWordsheet(OwnerId ownerId, String classId, String name);

    boolean renameWordsheet(OwnerId ownerId, String classId, int wordsheetId, String name);

    boolean makeWordsheetIsShared(OwnerId ownerId, String classId, int wordsheetId, boolean isShared);

    boolean deleteWordsheet(OwnerId ownerId, String classId, int wordsheetId);

    List<Word> getWords(OwnerId ownerId, String classId, int wordsheetId);

    Word addToWordsheet(OwnerId ownerId, String classId, int wordsheetId, int wordId);

    boolean removeFromWordsheet(OwnerId ownerId, String classId, int wordsheetId, int wordId);

}
