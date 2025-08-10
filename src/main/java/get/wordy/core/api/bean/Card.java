package get.wordy.core.api.bean;

import get.wordy.core.dao.exception.InconsistentDataException;

import java.util.Objects;

public class Card {

    private Integer vocabId;
    private Word word;
    private Progress progress;

    public int getVocabId() {
        return vocabId;
    }

    public void setVocabId(Integer vocabId) {
        this.vocabId = vocabId;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public Word getWord() {
        return this.word;
    }

    public void setProgress(Progress progress) {
        if (!Objects.equals(this.word.getId(), progress.getWordId())) {
            throw new InconsistentDataException("Progress.wordId and Word.id are not consistent");
        }
        this.progress = progress;
    }

    public Progress getProgress() {
        return progress;
    }

    public Integer getWordId() {
        return this.word.getId();
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