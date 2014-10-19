package get.wordy.core.api;

import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.wrapper.CardItem;

import java.util.List;
import java.util.Map;

public interface IDictionaryService {

    List<Dictionary> getDictionaries();

    boolean createDictionary(final String dictionaryName);

    boolean removeDictionary(final String dictionaryName);

    boolean renameDictionary(final String oldDictionaryName, final String newDictionaryName);

    Map<String, CardItem> getCards(String dictionaryName);

    Map<String, CardItem> getCardsForExercise(String dictionaryName, final int amount);

    boolean saveOrUpdateCard(CardItem cardItem);

    boolean removeCard(final String word);

    CardItem loadCard(final String word);

    boolean changeStatus(final String word, CardStatus updatedStatus);

    IScore getScore(final String dictionaryName);

    boolean resetScore(String dictionaryName);

    boolean increaseScoreUp(final String word, int repetitions);

}