package get.wordy.core.bean;

import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.wrapper.GramUnit;
import get.wordy.core.bean.xml.TimestampAdapter;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        assertXMLEqual(getSampleXML(), cardXML);
        //assertXMLEqual(readFile("src/test/resources/Card.xml", StandardCharsets.UTF_8), cardXML);
    }

    private String getSampleXML() {
        String xml = "<?xml version=\"1.0\"?>" +
                "<card id=\"1234\" dictionary-id=\"5\">" +
                "<word id=\"1234\"><value>Apple</value><transcription>apple</transcription></word>" +
                "<status>EDIT</status>" +
                "<rating>0</rating>" +
                "<insert-time>2010-07-15 16:16:39</insert-time>" +
                "<update-time>2010-07-17 13:33:56</update-time>" +
                "<definitions>" +
                "<definition id=\"4321\" card-id=\"1234\"><gram-unit>NOUN</gram-unit><value>Kind of sweet fruit</value>" +
                "<meanings>" +
                "<meaning id=\"9999\" definition-id=\"4321\"><translation></translation><synonym></synonym><antonym></antonym>" +
                "<example>Bad apple rotten apple - a person with a corrupting influence</example></meaning>" +
                "</meanings></definition></definitions></card>";
        return xml;
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding).replaceAll("\\r\\n", "");
    }

}