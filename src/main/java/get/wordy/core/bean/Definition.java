package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.GramUnit;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * @since 1.0
 */
@XmlRootElement
@XmlType(propOrder = {"gramUnit", "value", "meanings"})
public class Definition extends Parent<Meaning> {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Definition that = (Definition) o;

        return Objects.equals(this.cardId, that.cardId)
                && Objects.equals(this.id, that.id)
                && Objects.equals(this.gramUnit, that.gramUnit)
                && Objects.equals(this.value, that.value)
                && Objects.deepEquals(this.getChildren(), that.getChildren());
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + Objects.hashCode(gramUnit);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + cardId;
        return result;
    }

    @Override
    protected Definition clone() {
        Definition clone;
        try {
            clone = (Definition) super.clone();
            Iterator<Meaning> iterator = getMeanings().iterator();
            clone.children.clear();
            while (iterator.hasNext()) {
                clone.add(iterator.next().clone());
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return clone;
    }

}