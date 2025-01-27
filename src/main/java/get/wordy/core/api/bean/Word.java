package get.wordy.core.api.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Word {

    private int id;
    private String value;
    private String partOfSpeech;
    private String transcription;
    private String meaning;
    private final List<InContext> sentences = new ArrayList<>();
    private final List<String> collocations = new ArrayList<>();

    public Word() {
    }

    public Word(String value, String partOfSpeech, String transcription, String meaning) {
        this.value = value;
        this.partOfSpeech = partOfSpeech;
        this.transcription = transcription;
        this.meaning = meaning;
    }

    public Word(int id, String value, String partOfSpeech, String transcription, String meaning) {
        this(value, partOfSpeech, transcription, meaning);
        this.id = id;
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
        this.id = id;
        return this;
    }

    public List<InContext> getSentences() {
        return List.copyOf(sentences);
    }

    public List<String> getStrSentences() {
        return sentences
                .stream()
                .map(InContext::getExample)
                .toList();
    }

    public void addSentence(InContext sentence) {
        sentences.add(sentence);
    }

    public void addStrSentence(String sentence) {
        sentences.add(InContext.of(sentence));
    }

    public void setSentences(List<InContext> sentences) {
        this.sentences.clear();
        this.sentences.addAll(sentences);
    }

    public void setStrSentences(List<String> strSentences) {
        this.sentences.clear();
        this.sentences.addAll(strSentences
                .stream()
                .map(InContext::of)
                .toList());
    }

    public List<String> getCollocations() {
        return List.copyOf(collocations);
    }

    public void addCollocation(String collocation) {
        collocations.add(collocation);
    }

    public void setCollocations(List<String> collocations) {
        this.collocations.clear();
        this.collocations.addAll(collocations);
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