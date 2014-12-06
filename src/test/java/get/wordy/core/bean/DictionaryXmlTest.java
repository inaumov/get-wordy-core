package get.wordy.core.bean;

import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

public class DictionaryXmlTest extends XMLTestCase {

    @Test
    public void testConvertDictionaryToXML() throws Exception {
        Dictionary dictionary = new Dictionary(1234, "Fruits");
        String dictionaryXML = dictionary.toXml();
        assertXMLEqual("Comparing test xml to control xml", getSampleXML(), dictionaryXML);
    }

    private String getSampleXML() {
        return "<?xml version=\"1.0\"?><dictionary id=\"1234\"><name>Fruits</name></dictionary>";
    }

}