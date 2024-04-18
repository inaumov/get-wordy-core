package get.wordy.core.api.bean;

import org.springframework.util.CollectionUtils;

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
        if (sentence == null) {
            return;
        }
        sentences.add(sentence);
    }

    public void setSentences(List<Sentence> sentences) {
        if (CollectionUtils.isEmpty(sentences)) {
            this.sentences.clear();
            return;
        }
        this.sentences.clear();
        this.sentences.addAll(sentences);
    }

}