package get.wordy.core.api.bean;

public record Sentence(String example, String matchedWords) {

    public static Sentence of(String fullSentence) {
        return new Sentence(fullSentence, null);
    }

    public Sentence withMatchedWords(String matchedWords) {
        return new Sentence(this.example, matchedWords);
    }

}