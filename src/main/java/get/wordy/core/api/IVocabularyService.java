package get.wordy.core.api;

import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.exception.VocabNotFoundException;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IVocabularyService {

    List<Vocabulary> getVocabularies(OwnerId ownerId);

    Vocabulary createVocabulary(OwnerId ownerId, String name, String pictureUrl);

    boolean renameVocabulary(OwnerId ownerId, int vocabId, String newName);

    boolean changeVocabularyPicture(OwnerId ownerId, int vocabId, String newPictureUrl);

    boolean makeVocabularyIsShared(OwnerId ownerId, int vocabId, boolean isShared);

    boolean deleteVocabulary(OwnerId ownerId, int vocabId);

    List<Word> getWords(OwnerId ownerId, int vocabId);

    Word addToVocabulary(OwnerId ownerId, int vocabId, int wordRef);

    boolean removeFromVocabulary(OwnerId ownerId, int vocabId, int wordRef);

    boolean hasVocabulary(OwnerId ownerId, int vocabId) throws VocabNotFoundException;

}
