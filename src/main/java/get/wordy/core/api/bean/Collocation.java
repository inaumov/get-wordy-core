package get.wordy.core.api.bean;

public class Collocation {
    private int id;
    private String example;
    private int cardId;

    public Collocation() {
    }

    public Collocation(int id, String example, int cardId) {
        this.id = id;
        this.example = example;
        this.cardId = cardId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public Collocation withId(int id) {
        return new Collocation(id, example, cardId);
    }
}