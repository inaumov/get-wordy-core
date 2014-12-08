package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.xml.JaxbXMLHelper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "cards")
@XmlAccessorType(XmlAccessType.FIELD)
public class Cards {

    @XmlElement(name = "card")
    private List<Card> cards = null;

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public String toXml() {
        return new JaxbXMLHelper().convertFromPojoToXML(this, this.getClass());
    }

}