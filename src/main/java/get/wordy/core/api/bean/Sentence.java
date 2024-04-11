package get.wordy.core.api.bean;

public class Sentence {

    private String example;
    private String matchedWord;
    private int cardId;

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

    public String getMatchedWord() {
        return matchedWord;
    }

    public void setMatchedWord(String matchedWord) {
        this.matchedWord = matchedWord;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public static Sentence of(String fullSentence) {
        return new Sentence(fullSentence);
    }

}