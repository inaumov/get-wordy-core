package get.wordy.core.api.bean;

public class Collocation {
    private String example;
    private int cardId;

    public Collocation() {
    }

    public Collocation(String example, int cardId) {
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