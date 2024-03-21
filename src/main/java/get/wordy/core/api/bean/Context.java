package get.wordy.core.api.bean;

public class Context {

    private int id;
    private String example;
    private int cardId;

    public Context() {
    }

    public Context(int id, String example, int cardId) {
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

    public Context withId(int id) {
        return new Context(id, example, cardId);
    }

}