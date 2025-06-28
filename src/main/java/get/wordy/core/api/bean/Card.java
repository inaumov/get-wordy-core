package get.wordy.core.api.bean;

import get.wordy.core.dao.exception.InconsistentDataException;

import java.time.Instant;
import java.util.Objects;

public class Card {

    private int id;
    private int vocabId;
    private int wordId;
    private CardStatus status = CardStatus.TO_LEARN;
    private int score;
    private Instant insertedAt;
    private Instant updatedAt;
    private Word word;

    public int getId() {
        return id;
    }

    public void setId(int cardId) {
        this.id = cardId;
    }

    public int getVocabId() {
        return vocabId;
    }

    public void setVocabId(int vocabId) {
        this.vocabId = vocabId;
    }

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
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

        return this.id == that.id
                && this.wordId == that.wordId
                && this.vocabId == that.vocabId
                && this.status == that.status
                && this.score == that.score;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, wordId, vocabId, status, score);
    }

}