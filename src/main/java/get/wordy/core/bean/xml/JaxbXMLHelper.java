package get.wordy.core.bean.xml;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class JaxbXMLHelper {

    public <T> T convertFromXMLToPojo(String fromXml, Class<T> type) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(type.getClass());
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(new StringReader(fromXml));
            Object obj = unmarshaller.unmarshal(source, type).getValue();
            return type.cast(obj);
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext for creating the object from XML.");
        }
    }

    public String convertFromPojoToXML(Object o, Class<?> type) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(type);
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(o, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext for formatting the object to XML.");
        }
    }

}