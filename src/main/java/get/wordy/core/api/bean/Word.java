package get.wordy.core.api.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Word {

    private Integer id;
    private String lemma;
    private String partOfSpeech;
    private String transcription;
    private String meaning;
    private String register;
    private String domain;
    private final List<Sentence> sentences = new ArrayList<>();
    private final List<String> collocations = new ArrayList<>();

    public Word() {
    }

    public Word(String lemma, String partOfSpeech, String transcription, String meaning) {
        this.lemma = lemma;
        this.partOfSpeech = partOfSpeech;
        this.transcription = transcription;
        this.meaning = meaning;
    }

    public Word(Integer id, String lemma, String partOfSpeech, String transcription, String meaning) {
        this(lemma, partOfSpeech, transcription, meaning);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
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

    public Word withId(Integer id) {
        this.id = id;
        return this;
    }

    public String getRegister() {
        return register;
    }

    public void setRegister(String register) {
        this.register = register;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<Sentence> getSentences() {
        return List.copyOf(sentences);
    }

    public List<String> getStrSentences() {
        return sentences
                .stream()
                .map(Sentence::example)
                .toList();
    }

    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }

    public void addStrSentence(String sentence) {
        sentences.add(Sentence.of(sentence));
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences.clear();
        this.sentences.addAll(sentences);
    }

    public void setStrSentences(List<String> strSentences) {
        this.sentences.clear();
        this.sentences.addAll(strSentences
                .stream()
                .map(Sentence::of)
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
        return Objects.equals(id, that.id)
                && Objects.equals(lemma, that.lemma)
                && Objects.equals(partOfSpeech, that.partOfSpeech)
                && Objects.equals(transcription, that.transcription)
                && Objects.equals(meaning, that.meaning);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lemma, partOfSpeech, transcription, meaning);
    }

}