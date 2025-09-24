package get.wordy.core.api.bean;

import java.util.List;

public record FlashCard(
        Integer wordId,
        String lemma,
        String transcription,
        String partOfSpeech,
        String meaning,
        List<Sentence> sentences
) {
    public List<String> getStrSentences() {
        return sentences
                .stream()
                .map(Sentence::example)
                .toList();
    }

}