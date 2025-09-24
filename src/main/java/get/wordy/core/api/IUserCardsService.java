package get.wordy.core.api;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.FlashCard;
import get.wordy.core.api.bean.Progress;
import get.wordy.core.api.bean.wrapper.Score;
import get.wordy.core.api.id.OwnerId;

import java.util.List;

public interface IUserCardsService {

    List<Card> getCards(OwnerId ownerId, int vocabId);

    List<Progress> getProgress(OwnerId ownerId, int vocabId);

    List<FlashCard> pickFlashCards(OwnerId ownerId, int vocabId, int limit);

    void saveProgress(OwnerId ownerId, int vocabId, int[] wordsRefs, int repetitions);

    boolean resetProgress(OwnerId ownerId, int vocabId, int wordId);

    Score getProgressSummary(OwnerId ownerId, int vocabId);

}