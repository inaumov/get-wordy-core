package get.wordy.core.bean;

import get.wordy.core.bean.xml.XmlUtil;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class DictionaryTest extends XMLTestCase {

    @Test
    public void testConvertDictionaryToXML() throws Exception {
        Dictionary dictionary = new Dictionary(1234, "Fruits");
        String dictionaryXML = dictionary.toXml();
        String control = XmlUtil.readFile("src/test/resources/Dictionary.xml", StandardCharsets.UTF_8);
        assertXMLEqual("Comparing test xml to control xml", control, dictionaryXML);
    }

}