package get.wordy.core.api.bean;

import java.util.Objects;

public class Sentence {

    private int wordId;
    private String example;
    private String matchedWords;

    public Sentence(String fullSentence) {
        this.example = fullSentence;
    }

    public Sentence(String example, int wordId) {
        this.wordId = wordId;
        this.example = example;
    }

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
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

        return wordId == sentence.wordId
                && Objects.equals(example, sentence.example)
                && Objects.equals(matchedWords, sentence.matchedWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wordId, example, matchedWords);
    }

}