package get.wordy.core.bean;

import get.wordy.core.bean.xml.JaxbXMLHelper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since 1.0
 */
@XmlRootElement
public class Dictionary {

    private int id;

    private String name;

    public Dictionary() {
    }

    public Dictionary(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @XmlAttribute
    public int getId() {
        return id;
    }

    public void setId(int dictionaryId) {
        this.id = dictionaryId;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String dictionary) {
        this.name = dictionary;
    }

    public String toXml() {
        return new JaxbXMLHelper().convertFromPojoToXML(this, this.getClass());
    }

}