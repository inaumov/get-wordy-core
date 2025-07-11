package get.wordy.core.api.bean;

import get.wordy.core.dao.exception.InconsistentDataException;

import java.time.Instant;
import java.util.Objects;

public class Card {

    private Integer id;
    private Integer vocabId;
    private Integer wordId;
    private CardStatus status;
    private int score;
    private Instant insertedAt;
    private Instant updatedAt;
    private Word word;

    public Integer getId() {
        return id;
    }

    public void setId(Integer cardId) {
        this.id = cardId;
    }

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

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        if (wordId != word.getId()) {
            throw new InconsistentDataException("Card.wordId and Word.id are not consistent");
        }
        this.word = word;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card that = (Card) o;

        return Objects.equals(this.id, that.id)
                && Objects.equals(this.vocabId, that.vocabId)
                && Objects.equals(this.wordId, that.wordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vocabId, wordId);
    }

}