package get.wordy.core.bean;

public class Dictionary {

    private int id;

    private String name;

    public Dictionary() {
    }

    public Dictionary(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int dictionaryId) {
        this.id = dictionaryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String dictionary) {
        this.name = dictionary;
    }

}