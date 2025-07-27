package get.wordy.core.api.bean;

import get.wordy.core.dao.exception.InconsistentDataException;

import java.util.Objects;

public class Card extends Progress {

    private Word word;

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        if (!Objects.equals(super.getWordId(), word.getId())) {
            throw new InconsistentDataException("Progress.wordId and Word.id are not consistent");
        }
        this.word = word;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}