package get.wordy.core.api;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.wrapper.Score;

import java.util.List;
import java.util.Set;

public interface IDictionaryService {

    List<Dictionary> getDictionaries();

    Dictionary createDictionary(final String dictionaryName, String picture);

    boolean removeDictionary(final int dictionaryId);

    boolean renameDictionary(final int dictionaryId, final String newDictionaryName);

    Set<Card> getCards(int dictionaryId);

    List<Card> getCardsForExercise(int dictionaryId, final int amount);

    boolean addCard(int dictionaryId, Card card);

    boolean updateCard(int dictionaryId, Card card);

    boolean removeCard(final int cardId);

    Card loadCard(final int cardId);

    boolean changeStatus(final int cardId, CardStatus updatedStatus);

    Score getScore(final int dictionaryId);

    boolean resetScore(int dictionaryId);

    boolean increaseScoreUp(final int cardId, int repetitions);

    List<Card> generateCards(int dictionaryId, Set<String> words);

}