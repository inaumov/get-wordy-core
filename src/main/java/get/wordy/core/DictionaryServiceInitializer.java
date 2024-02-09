package get.wordy.core;

import get.wordy.core.dao.impl.CardDao;
import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.dao.impl.DictionaryDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.ConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DictionaryServiceInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryServiceInitializer.class);

    private static final String DEFAULT_URI = "sql/ServerInfo.xml";

    private DictionaryDao dictionaryDao;
    private WordDao wordDao;
    private CardDao cardDao;
    private ConnectionWrapper connection;

    public DictionaryServiceInitializer(String uri) throws IOException {
        if (uri == null || uri.isEmpty()) {
            uri = DEFAULT_URI;
        }
        ServerInfoParser serverInfoParser = new ServerInfoParser();
        serverInfoParser.parseDocument(uri);
        ServerInfo serverInfo = serverInfoParser.getServerInfo();

        initDaoLayer(serverInfo);
    }

    public DictionaryServiceInitializer(ServerInfo customInfo) {
        initDaoLayer(customInfo);
    }

    private void initDaoLayer(ServerInfo serverInfo) {
        if (serverInfo == null) {
            throw new NullPointerException(ServerInfo.class.getSimpleName() + "is not properly set");
        }
        if (connection == null) {
            connection = ConnectionWrapper.getInstance();
        }
        connection.setServerInfo(serverInfo);

        DaoFactory daoFactory = DaoFactory.getFactory();

        dictionaryDao = daoFactory.getDictionaryDao();
        wordDao = daoFactory.getWordDao();
        cardDao = daoFactory.getCardDao();
    }

}