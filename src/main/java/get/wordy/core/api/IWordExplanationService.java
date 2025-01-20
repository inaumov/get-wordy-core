package get.wordy.core.api;

import get.wordy.core.api.bean.Word;

public interface IWordExplanationService {

    Word getWordExplanation(int wordId);

    Word addWordExplanation(Word entity);

    Word updateWordExplanation(Word entity);

    boolean deleteWordExplanationPermanently(int wordId);

}
