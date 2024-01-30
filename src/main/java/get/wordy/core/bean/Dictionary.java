package get.wordy.core.bean;

public class Dictionary {

    private int id;

    private String name;

    private String picture;

    public Dictionary() {
    }

    public Dictionary(int id, String name, String picture) {
        this.id = id;
        this.name = name;
        this.picture = picture;
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

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

}