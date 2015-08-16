package get.wordy.core;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

public class ServerInfoParser extends DefaultHandler {

    private String tempVal;
    private ServerInfo serverInfo;
    private boolean isAdmin = false;

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void parseDocument(String uri) throws IOException {
        // get a factory
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        try {
            // get a new instance of parser
            SAXParser saxParser = saxFactory.newSAXParser();

            // parse the file and also register this class for callbacks
            saxParser.parse(uri, this);

        } catch (SAXException e) {
            // TODO: handle exception
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            throw new IOException("Probably no sql/ServerInfo.xml configuration either was found or parsed", e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // reset
        tempVal = "";
        if (qName.equalsIgnoreCase("admin")) {
            isAdmin = true;
        } else if (qName.equalsIgnoreCase("user")) {
            if (!isAdmin) {
                throw new SAXException("User tag is not expected here.");
            }
        } else if (qName.equalsIgnoreCase("password")) {
            if (!isAdmin) {
                throw new SAXException("Password tag is not expected here.");
            }
        } else if (qName.equalsIgnoreCase("server")) {
            // create a new instance of ServerInfo when parse outer tag
            serverInfo = new ServerInfo();
            serverInfo.setHost(attributes.getValue("host"));
        } else if (qName.equalsIgnoreCase("database")) {
            serverInfo.setDatabase(attributes.getValue("name"));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("admin")) {
            isAdmin = false;
        } else if (qName.equalsIgnoreCase("user")) {
            serverInfo.getAccount().setProperty("user", tempVal);
        } else if (qName.equalsIgnoreCase("password")) {
            serverInfo.getAccount().setProperty("password", tempVal);
        } else if (qName.equalsIgnoreCase("parameter")) {
            serverInfo.addParameter(tempVal);
        }
    }

}