package get.wordy.core.bean;

import javax.xml.bind.annotation.*;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Word {

    @XmlAttribute
    private int id;

    @XmlElement
    private String value;

    @XmlElement
    private String transcription;

    public Word() {
    }

    public Word(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Word obj = (Word) o;

        if (id != obj.id) return false;
        if (!value.equals(obj.value)) return false;
        if (!transcription.equals(obj.transcription)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + value.hashCode();
        result = 31 * result + transcription.hashCode();
        return result;
    }

}