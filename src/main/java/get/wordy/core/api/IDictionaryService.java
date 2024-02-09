package get.wordy.core.api;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.wrapper.Score;

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

    Score getScore(int dictionaryId);

    boolean resetScore(int dictionaryId);

    boolean increaseScoreUp(int cardId, int repetitions);

    List<Card> generateCards(int dictionaryId, Set<String> words);

}