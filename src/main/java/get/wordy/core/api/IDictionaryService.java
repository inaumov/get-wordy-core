package get.wordy.core.api;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.Exercise;
import get.wordy.core.api.bean.wrapper.Score;
import get.wordy.core.api.id.OwnerId;

import java.util.List;
import java.util.Set;

public interface IDictionaryService {

    List<Card> getCards(OwnerId ownerId, int vocabId);

    List<Exercise> getCardsForExercise(OwnerId ownerId, int vocabId, int limit);

    Card addCard(OwnerId ownerId, int vocabId, int wordId);

    Card loadCard(int cardId);

    boolean deleteCard(OwnerId ownerId, int cardId);

    boolean resetScore(OwnerId ownerId, int cardId);

    boolean increaseScoreUp(OwnerId ownerId, int[] cardIds, int repetitions);

    List<Card> generateCards(OwnerId ownerId, int vocabId, Set<Integer> wordRefs);

    Score getScoreSummary(OwnerId ownerId, int vocabId);

}