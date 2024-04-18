package get.wordy.core.api.bean;

import java.util.Objects;

public class Sentence {

    private String example;
    private int cardId;
    private String matchedWords;

    public Sentence(String fullSentence) {
        this.example = fullSentence;
    }

    public Sentence(String example, int cardId) {
        this.example = example;
        this.cardId = cardId;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getMatchedWords() {
        return matchedWords;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public Sentence withMatchedWords(String matchedWords) {
        this.matchedWords = matchedWords;
        return this;
    }

    public static Sentence of(String fullSentence) {
        return new Sentence(fullSentence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sentence sentence = (Sentence) o;

        return Objects.equals(example, sentence.example)
                && Objects.equals(matchedWords, sentence.matchedWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(example, matchedWords);
    }

}