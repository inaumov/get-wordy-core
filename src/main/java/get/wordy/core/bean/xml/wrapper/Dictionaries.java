package get.wordy.core.bean.xml.wrapper;

import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.xml.JaxbXMLHelper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "dictionaries")
@XmlAccessorType(XmlAccessType.FIELD)
public class Dictionaries {

    @XmlElement(name = "dictionary")
    private List<Dictionary> dictionaries = null;

    public List<Dictionary> getDictionaries() {
        return dictionaries;
    }

    public void setDictionaries(List<Dictionary> dictionaries) {
        this.dictionaries = dictionaries;
    }

    public String toXml() {
        return new JaxbXMLHelper().convertFromPojoToXML(this, this.getClass());
    }

}