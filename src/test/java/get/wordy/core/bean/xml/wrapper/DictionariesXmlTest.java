package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Dictionary;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.util.ArrayList;

public class DictionariesXmlTest extends XMLTestCase {

    @Test
    public void testConvertDictionariesListToXML() throws Exception {
        Dictionaries dictionaries = new Dictionaries();
        dictionaries.setDictionaries(new ArrayList<Dictionary>());

        Dictionary dic1 = new Dictionary(1234, "Fruits");
        Dictionary dic2 = new Dictionary(5678, "Jobs");

        dictionaries.getDictionaries().add(dic1);
        dictionaries.getDictionaries().add(dic2);

        String dictionariesXML = dictionaries.toXml();

        assertXMLEqual("Comparing test xml to control xml", getSampleXML(), dictionariesXML);
    }

    private String getSampleXML() {
        return "<?xml version=\"1.0\"?><dictionaries>" +
                "<dictionary id=\"1234\"><name>Fruits</name></dictionary>" +
                "<dictionary id=\"5678\"><name>Jobs</name></dictionary>" +
                "</dictionaries>";
    }

}