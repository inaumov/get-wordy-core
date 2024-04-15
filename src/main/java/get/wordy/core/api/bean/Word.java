package get.wordy.core.api.bean;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word that = (Word) o;
        return id == that.id
                && Objects.equals(value, that.value)
                && Objects.equals(partOfSpeech, that.partOfSpeech)
                && Objects.equals(transcription, that.transcription)
                && Objects.equals(meaning, that.meaning);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, partOfSpeech, transcription, meaning);
    }

}