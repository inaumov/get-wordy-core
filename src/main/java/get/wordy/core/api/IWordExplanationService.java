package get.wordy.core.api;

import get.wordy.core.api.bean.Word;
import get.wordy.core.api.id.OwnerId;

public interface IWordExplanationService {

    Word getWordExplanation(OwnerId ownerId, int wordId);

    Word addWordExplanation(OwnerId ownerId, int vocabId, Word entity);

    Word updateWordExplanation(OwnerId ownerId, int vocabId, Word entity);

    void deleteWordExplanationPermanently(OwnerId ownerId, int vocabId, int wordId);

}
