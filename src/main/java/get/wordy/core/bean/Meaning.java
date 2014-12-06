package get.wordy.core.bean;

import javax.xml.bind.annotation.*;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"translation", "synonym", "antonym", "example"})
public class Meaning {

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

}