package get.wordy.core.api.bean;

import java.util.ArrayList;
import java.util.List;

public class Exercise {

    private int cardId;
    private int wordId;
    private Word word;
    private final List<Sentence> sentences = new ArrayList<>();

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public List<Sentence> getSentences() {
        return List.copyOf(sentences);
    }

    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences.clear();
        this.sentences.addAll(sentences);
    }

}