package get.wordy.core.api.bean;

import get.wordy.core.dao.exception.InconsistentDataException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Card {

    private int id;
    private int dictionaryId;
    private int wordId;
    private CardStatus status = CardStatus.DEFAULT_STATUS;
    private int score;
    private Instant insertedAt;
    private Instant updatedAt;
    private Word word;
    private final List<Context> contexts = new ArrayList<>();
    private final List<Collocation> collocations = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int cardId) {
        this.id = cardId;
    }

    public int getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(int dictionaryId) {
        this.dictionaryId = dictionaryId;
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
            throw new InconsistentDataException("Card.wordId and Word.Id are not consistent");
        }
        this.word = word;
    }

    public List<Context> getContexts() {
        return List.copyOf(contexts);
    }

    public void addContext(Context bean) {
        contexts.add(bean);
    }

    public void setContexts(List<Context> beans) {
        this.contexts.clear();
        this.contexts.addAll(beans);
    }

    public List<Collocation> getCollocations() {
        return List.copyOf(collocations);
    }

    public void addCollocation(Collocation bean) {
        collocations.add(bean);
    }

    public void setCollocations(List<Collocation> beans) {
        this.collocations.clear();
        this.collocations.addAll(beans);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card that = (Card) o;

        return Objects.equals(this.id, that.id)
                && Objects.equals(this.wordId, that.wordId)
                && Objects.equals(this.dictionaryId, that.dictionaryId)
                && Objects.equals(this.status, that.status)
                && Objects.equals(this.score, that.score)
                && Objects.equals(this.insertedAt, that.insertedAt)
                && Objects.equals(this.updatedAt, that.updatedAt)
                && Objects.deepEquals(this.contexts, that.getContexts())
                && Objects.deepEquals(this.collocations, that.getCollocations());
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + wordId;
        result = 31 * result + dictionaryId;
        return result;
    }

}