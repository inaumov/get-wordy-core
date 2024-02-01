package get.wordy.core.bean;

public class Word {
    private int id;
    private String value;
    private String partOfSpeech;
    private String transcription;
    private String meaning;

    public Word() {
    }

    public Word(int id, String value, String partOfSpeech, String transcription, String meaning) {
        this.id = id;
        this.value = value;
        this.partOfSpeech = partOfSpeech;
        this.transcription = transcription;
        this.meaning = meaning;
    }

    public int getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getTranscription() {
        return transcription;
    }

    public String getMeaning() {
        return meaning;
    }

    public Word withId(int id) {
        return new Word(id, value, partOfSpeech, transcription, meaning);
    }

}