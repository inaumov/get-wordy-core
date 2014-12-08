package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.xml.JaxbXMLHelper;
import get.wordy.core.bean.xml.TimestampAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.sql.Timestamp;
import java.util.List;

/**
 * @since 1.0
 */

@XmlRootElement
@XmlType(propOrder = {"word", "status", "rating", "insertTime", "updateTime", "definitions"})
public class Card extends ChildrenHolder<Definition> {

    private int id;
    private int dictionaryId;
    private int wordId;
    private CardStatus status = CardStatus.DEFAULT_STATUS;
    private int rating;
    private Timestamp insertTime;
    private Timestamp updateTime;
    private Word word;

    @XmlAttribute
    public int getId() {
        return id;
    }

    public void setId(int cardId) {
        this.id = cardId;
    }

    @XmlAttribute(name = "dictionary-id")
    public int getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(int dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    @XmlTransient
    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
    }

    @XmlElement
    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @XmlElement
    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    @XmlElement(name = "insert-time")
    @XmlJavaTypeAdapter(value = TimestampAdapter.class, type = Timestamp.class)
    public Timestamp getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    @XmlElement(name = "update-time")
    @XmlJavaTypeAdapter(value = TimestampAdapter.class, type = Timestamp.class)
    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    @XmlElement(required = true)
    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        if (word.getId() != wordId) {
            throw new InconsistentDataException("Card and Word objects are not consistent with their wordId");
        }
        this.word = word;
    }

    @XmlElement(name = "definition", required = false)
    @XmlElementWrapper
    public List<Definition> getDefinitions() {
        return getChildren();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        if (id != card.id) return false;
        if (wordId != card.wordId) return false;
        if (dictionaryId != card.dictionaryId) return false;
        if (status != card.status) return false;
        if (rating != card.rating) return false;
        if (!insertTime.equals(card.insertTime)) return false;
        if (!updateTime.equals(card.updateTime)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + wordId;
        result = 31 * result + dictionaryId;
        return result;
    }

    public String toXml() {
        return new JaxbXMLHelper().convertFromPojoToXML(this, Card.class);
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