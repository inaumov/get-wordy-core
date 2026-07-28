package get.wordy.core.api;

import get.wordy.core.api.bean.ExistingWordLookup;
import get.wordy.core.api.bean.Word;
import get.wordy.core.api.bean.WordKey;

import java.util.List;

public interface IWordExplanationService {

    List<Word> findExplanations(String value);

    Word getWordExplanation(int wordId);

    Word addWordExplanation(Word entity);

    Word updateWordExplanation(Word entity);

    boolean deleteWordExplanationPermanently(int wordId);

    List<ExistingWordLookup> lookupWords(List<WordKey> words);

    List<ExistingWordLookup> findExistingWords(List<WordKey> words);
}
