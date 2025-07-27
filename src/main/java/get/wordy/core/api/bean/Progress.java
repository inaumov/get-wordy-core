package get.wordy.core.api.bean;

import java.time.Instant;
import java.util.Objects;

public class Progress {

    private Integer vocabId;
    private Integer wordId;
    private CardStatus status;
    private int score;
    private Instant insertedAt;
    private Instant updatedAt;

    public Integer getVocabId() {
        return vocabId;
    }

    public void setVocabId(Integer vocabId) {
        this.vocabId = vocabId;
    }

    public Integer getWordId() {
        return wordId;
    }

    public void setWordId(Integer wordId) {
        this.wordId = wordId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Instant getInsertedAt() {
        return insertedAt;
    }

    public void setInsertedAt(Instant insertedAt) {
        this.insertedAt = insertedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Progress that = (Progress) o;

        return Objects.equals(this.vocabId, that.vocabId)
                && Objects.equals(this.wordId, that.wordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vocabId, wordId);
    }

}