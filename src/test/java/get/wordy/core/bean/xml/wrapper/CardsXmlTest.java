package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.xml.XmlUtil;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
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
        String control = XmlUtil.readFile("src/test/resources/Cards.xml", StandardCharsets.UTF_8);
        assertXMLEqual("Comparing test xml to control xml", control, cardsXML);
    }

}