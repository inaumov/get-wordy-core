package get.wordy.core;

import junit.framework.TestCase;
import org.junit.Test;

public class ServerInfoParserTest extends TestCase {

    @Test
    public void testServerInfoParser() throws Exception {
        ServerInfoParser serverInfoParser = new ServerInfoParser();
        serverInfoParser.parseDocument("src/test/resources/TestServerInfo.xml");
        ServerInfo serverInfo = serverInfoParser.getServerInfo();
        assertEquals("host:7777", serverInfo.getHost());
        assertEquals("test_db", serverInfo.getDatabase());

        assertTrue(2 == serverInfo.getAccount().size());
        assertEquals("usr", serverInfo.getAccount().getProperty("user"));
        assertEquals("pwd", serverInfo.getAccount().getProperty("password"));

        assertTrue(2 == serverInfo.getParameters().size());
        assertTrue(serverInfo.getParameters().contains("param1"));
        assertTrue(serverInfo.getParameters().contains("param2"));
    }
}
