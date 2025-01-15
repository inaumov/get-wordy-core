package get.wordy.core.api.bean;

public class Vocabulary {

    private int vocabId;

    private String name;

    private String pictureUrl;

    private boolean isShared;

    private int wordsTotal;

    public Vocabulary() {
    }

    public Vocabulary(String name, String pictureUrl) {
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public Vocabulary(int vocabId, String name, String pictureUrl, boolean isShared, int wordsTotal) {
        this.vocabId = vocabId;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.isShared = isShared;
        this.wordsTotal = wordsTotal;
    }

    public int getVocabId() {
        return vocabId;
    }

    public void setVocabId(int vocabId) {
        this.vocabId = vocabId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public int getWordsTotal() {
        return wordsTotal;
    }

    public void setWordsTotal(int wordsTotal) {
        this.wordsTotal = wordsTotal;
    }

}