package get.wordy.core.bean;

import javax.xml.bind.annotation.*;
import java.util.Objects;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"translation", "synonym", "antonym", "example"})
public class Meaning implements Cloneable {

    @XmlAttribute
    private int id;
    @XmlElement
    private String translation;
    @XmlElement
    private String synonym;
    @XmlElement
    private String antonym;
    @XmlElement
    private String example;
    @XmlAttribute(name = "definition-id")
    private int definitionId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getSynonym() {
        return synonym;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    public String getAntonym() {
        return antonym;
    }

    public void setAntonym(String antonym) {
        this.antonym = antonym;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Meaning that = (Meaning) o;

        return Objects.equals(this.definitionId, that.definitionId)
                && Objects.equals(this.id, that.id)
                && Objects.equals(this.antonym, that.antonym)
                && Objects.equals(this.example, that.example)
                && Objects.equals(this.synonym, that.synonym)
                && Objects.equals(this.translation, that.translation);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + Objects.hashCode(translation);
        result = 31 * result + Objects.hashCode(synonym);
        result = 31 * result + Objects.hashCode(antonym);
        result = 31 * result + Objects.hashCode(example);
        result = 31 * result + definitionId;
        return result;
    }

    @Override
    protected Meaning clone() {
        try {
            return (Meaning) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}