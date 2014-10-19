package get.wordy.core.wrapper;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Meaning;
import get.wordy.core.bean.Word;
import get.wordy.core.bean.wrapper.GramUnit;

import java.util.HashMap;

public class CardItem extends Card {

    private Word word;
    private HashMap<GramUnit, DefinitionPair<Definition, Meaning>> additions;

    public CardItem() {
        word = new Word();
    }

    public CardItem(Card card, Word word) {
        this();
        if (card.getWordId() != word.getId()) {
            throw new InconsistentDataException("Card and Word objects are not consistent with their wordId");
        }
        updateWord(word);
        updateCard(card);
    }

    private void updateWord(Word word) {
        this.word = word;
        setWordId(word.getId());
    }

    public void updateCard(Card card) {
        setId(card.getId());
        setRating(card.getRating());
        setStatus(card.getStatus());
        setDictionaryId(card.getDictionaryId());
        setInsertTime(card.getInsertTime());
        setUpdateTime(card.getUpdateTime());
    }

    public Word getWord() {
        return word;
    }

    public Card getCard() {
        return this;
    }

    public void addDefinitionPair(GramUnit gramUnit, DefinitionPair<Definition, Meaning> definitionPair) {
        GramUnit unit = definitionPair.getDefinition().getGramUnit();
        if (gramUnit != unit) {
            throw new InconsistentDataException("GramUnit key is not consistent with attached Definition object");
        }
        if (additions == null) {
            additions = new HashMap<GramUnit, DefinitionPair<Definition, Meaning>>(GramUnit.count());
        }
        additions.put(gramUnit, definitionPair);
    }

    public DefinitionPair<Definition, Meaning> getDefinitionPair(GramUnit gramUnit) {
        if (additions == null) {
            return null;
        }
        return additions.get(gramUnit);
    }

    private class InconsistentDataException extends RuntimeException {

        /**
         * Constructs a {@code InconsistentDataException} with no detail message.
         */
        public InconsistentDataException() {
            super();
        }

        /**
         * Constructs a {@code InconsistentDataException} with the specified
         * detail message.
         *
         * @param s the detail message.
         */
        public InconsistentDataException(String s) {
            super(s);
        }

    }

}