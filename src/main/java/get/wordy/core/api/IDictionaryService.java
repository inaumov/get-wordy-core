package get.wordy.core.api;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.api.bean.Exercise;
import get.wordy.core.api.bean.wrapper.Score;
import get.wordy.core.api.id.OwnerId;

import java.util.List;
import java.util.Set;

public interface IDictionaryService {

    List<Card> getCards(OwnerId ownerId, int dictionaryId);

    List<Exercise> getCardsForExercise(OwnerId ownerId, int dictionaryId, int limit);

    Card addCard(int dictionaryId, Card card);

    Card loadCard(int cardId);

    boolean deleteCard(OwnerId ownerId, int dictionaryId, int cardId);

    boolean changeStatus(int cardId, CardStatus updatedStatus);

    boolean resetScore(int cardId);

    boolean increaseScoreUp(int dictionaryId, int[] cardIds, int repetitions);

    List<Card> generateCards(OwnerId ownerId, int dictionaryId, Set<Integer> wordRefs);

    Score getScoreSummary(OwnerId ownerId, int dictionaryId);

}