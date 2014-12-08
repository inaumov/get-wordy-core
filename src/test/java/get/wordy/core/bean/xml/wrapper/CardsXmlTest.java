package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Card;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.util.ArrayList;

public class CardsXmlTest extends XMLTestCase {

    @Test
    public void testConvertCardsListToXML() throws Exception {
        Cards cards = new Cards();
        cards.setCards(new ArrayList<Card>());

        Card card1 = new Card();
        Card card2 = new Card();

        cards.getCards().add(card1);
        cards.getCards().add(card2);

        String cardsXML = cards.toXml();

        assertXMLEqual("Comparing test xml to control xml", getSampleXML(), cardsXML);
    }

    private String getSampleXML() {
        return "<?xml version=\"1.0\"?><cards>" +
                "<card id=\"0\" dictionary-id=\"0\"><status>EDIT</status><rating>0</rating><definitions/></card>" +
                "<card id=\"0\" dictionary-id=\"0\"><status>EDIT</status><rating>0</rating><definitions/></card>" +
                "</cards>";
    }

}