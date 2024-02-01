package get.wordy.core.bean;

public class Collocation {
    private int id;
    private String example;
    private int wordId;

    public Collocation() {
    }

    public Collocation(int id, String example, int wordId) {
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

    public Collocation withId(int id) {
        return new Collocation(id, example, wordId);
    }
}