package get.wordy.core.api.bean;

import java.util.Objects;

public class InContext {

    private String sentence;
    private String matchedWords;

    public InContext(String sentence) {
        this.sentence = sentence;
    }

    public String getExample() {
        return sentence;
    }

    public void setExample(String example) {
        this.sentence = example;
    }

    public String getMatchedWords() {
        return matchedWords;
    }

    public InContext withMatchedWords(String matchedWords) {
        this.matchedWords = matchedWords;
        return this;
    }

    public static InContext of(String fullSentence) {
        return new InContext(fullSentence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InContext sentence = (InContext) o;

        return Objects.equals(this.sentence, sentence.sentence)
                && Objects.equals(matchedWords, sentence.matchedWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sentence, matchedWords);
    }

}