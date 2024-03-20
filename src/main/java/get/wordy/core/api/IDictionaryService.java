package get.wordy.core.api;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.Dictionary;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.api.bean.wrapper.Score;

import java.util.List;
import java.util.Set;

public interface IDictionaryService {

    List<Dictionary> getDictionaries();

    Dictionary createDictionary(String dictionaryName, String picture);

    boolean removeDictionary(int dictionaryId);

    boolean renameDictionary(int dictionaryId, String newName);

    boolean changeDictionaryPicture(int dictionaryId, String newPictureUrl);

    List<Card> getCards(int dictionaryId);

    List<Card> getCardsForExercise(int dictionaryId, int limit);

    Card addCard(int dictionaryId, Card card);

    Card updateCard(int dictionaryId, Card card);

    boolean removeCard(int cardId);

    Card loadCard(int cardId);

    boolean changeStatus(int cardId, CardStatus updatedStatus);

    Score getScoreSummary(int dictionaryId);

    boolean resetScore(int cardId);

    boolean increaseScoreUp(int dictionaryId, int[] cardIds, int repetitions);

    List<Card> generateCards(int dictionaryId, Set<String> words);

}