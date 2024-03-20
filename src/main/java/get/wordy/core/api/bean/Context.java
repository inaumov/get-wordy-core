package get.wordy.core.api.bean;

public class Context {

    private int id;
    private String example;
    private int wordId;

    public Context() {
    }

    public Context(int id, String example, int wordId) {
        this.id = id;
        this.example = example;
        this.wordId = wordId;
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

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
    }

    public Context withId(int id) {
        return new Context(id, example, wordId);
    }

}