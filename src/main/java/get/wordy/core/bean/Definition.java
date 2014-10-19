package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.GramUnit;

import java.util.List;

/**
 * @since 1.0
 */
public class Definition extends ChildrenHolder<Meaning> {

    private int id;
    private GramUnit gramUnit;
    private String value;
    private int cardId;

    public int getId() {
        return id;
    }

    public void setId(int definitionId) {
        this.id = definitionId;
    }

    public GramUnit getGramUnit() {
        return gramUnit;
    }

    public void setGramUnit(GramUnit gramUnit) {
        this.gramUnit = gramUnit;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public List<Meaning> getMeanings() {
        return getChildren();
    }

}