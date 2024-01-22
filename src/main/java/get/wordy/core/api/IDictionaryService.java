package get.wordy.core.api;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.wrapper.Score;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IDictionaryService {

    List<Dictionary> getDictionaries();

    Dictionary createDictionary(final String dictionaryName);

    boolean removeDictionary(final int dictionaryId);

    boolean renameDictionary(final int dictionaryId, final String newDictionaryName);

    Map<String, Card> getCards(int dictionaryId);

    List<Card> getCardsForExercise(int dictionaryId, final int amount);

    boolean save(Card card);

    boolean removeCard(final String word);

    Card loadCard(final String word);

    boolean changeStatus(final String word, CardStatus updatedStatus);

    Score getScore(final int dictionaryId);

    boolean resetScore(int dictionaryId);

    boolean increaseScoreUp(final String word, int repetitions);

    boolean generateCards(int dictionaryId, Set<String> words);

}