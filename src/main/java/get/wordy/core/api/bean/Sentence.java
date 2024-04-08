package get.wordy.core.api.bean;

public class Sentence {

    private String example;
    private int cardId;

    public Sentence() {
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

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

}