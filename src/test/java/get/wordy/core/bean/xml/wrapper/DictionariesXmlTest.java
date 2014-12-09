package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.xml.XmlUtil;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
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

        String control = XmlUtil.readFile("src/test/resources/Dictionaries.xml", StandardCharsets.UTF_8);
        assertXMLEqual("Comparing test xml to control xml", control, dictionariesXML);
    }

}