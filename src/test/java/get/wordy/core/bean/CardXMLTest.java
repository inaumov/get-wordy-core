package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.wrapper.GramUnit;
import get.wordy.core.bean.xml.TimestampAdapter;
import get.wordy.core.bean.xml.XmlUtil;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CardXMLTest extends XMLTestCase {

    @Test
    public void testConvertFromPojoToXML() throws Exception {
        Card card = new Card();
        card.setId(1234);
        card.setDictionaryId(5);
        card.setRating(0);
        card.setStatus(CardStatus.DEFAULT_STATUS);
        String insertStr = "2010-07-15 16:16:39";
        String updateStr = "2010-07-17 13:33:56";
        DateFormat format = new SimpleDateFormat(TimestampAdapter.DATETIME_PATTERN, Locale.ENGLISH);
        card.setInsertTime(new Timestamp(format.parse(insertStr).getTime()));
        card.setUpdateTime(new Timestamp(format.parse(updateStr).getTime()));
        card.setWordId(1234);

        Word word = new Word(1234, "Apple");
        word.setTranscription("apple");
        card.setWord(word);

        Definition definition = new Definition();
        definition.setId(4321);
        definition.setValue("Kind of sweet fruit");
        definition.setGramUnit(GramUnit.NOUN);
        definition.setCardId(1234);
        Meaning meaning = new Meaning();
        meaning.setId(9999);
        meaning.setTranslation("");
        meaning.setSynonym("");
        meaning.setAntonym("");
        meaning.setExample("Bad apple rotten apple - a person with a corrupting influence");
        meaning.setDefinitionId(4321);
        definition.add(meaning);
        card.add(definition);

        String cardXML = card.toXml();
        String control = XmlUtil.readFile("src/test/resources/Card.xml", StandardCharsets.UTF_8);
        assertXMLEqual("Comparing test xml to control xml", control, cardXML);
    }

}