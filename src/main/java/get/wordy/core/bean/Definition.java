package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.GramUnit;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlType(propOrder = {"gramUnit", "value", "meanings"})
public class Definition extends ChildrenHolder<Meaning> {

    private int id;
    private GramUnit gramUnit;
    private String value;
    private int cardId;

    @XmlAttribute
    public int getId() {
        return id;
    }

    public void setId(int definitionId) {
        this.id = definitionId;
    }

    @XmlElement(name = "gram-unit")
    public GramUnit getGramUnit() {
        return gramUnit;
    }

    public void setGramUnit(GramUnit gramUnit) {
        this.gramUnit = gramUnit;
    }

    @XmlElement
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlAttribute(name = "card-id")
    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    @XmlElement(name = "meaning")
    @XmlElementWrapper
    public List<Meaning> getMeanings() {
        return getChildren();
    }

}