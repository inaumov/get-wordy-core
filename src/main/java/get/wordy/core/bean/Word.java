package get.wordy.core.bean;

import javax.xml.bind.annotation.*;
import java.util.Objects;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Word implements Cloneable {

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

    public Word(int id, String value, String transcription) {
        this(id, value);
        this.transcription = transcription;
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

        Word that = (Word) o;

        return Objects.equals(this.id, that.id)
                && Objects.equals(this.value, that.value)
                && Objects.equals(this.transcription, that.transcription);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(transcription);
        return result;
    }

    @Override
    protected Word clone() {
        try {
            return (Word) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}