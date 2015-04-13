package get.wordy.dao;

import get.wordy.core.ServerInfo;
import get.wordy.core.bean.Card;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Meaning;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.wrapper.GramUnit;
import get.wordy.core.dao.ADaoFactory;
import get.wordy.core.db.ConnectionFactory;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.List;

@Ignore
public class DaoTestBase extends TestCase {

    protected ConnectionFactory connect;
    protected static ADaoFactory factory;

    @Override
    @Before
    public void setUp() throws Exception {

        // Set test server info
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setHost(System.getProperty("host"));
        serverInfo.setDatabase(System.getProperty("database"));
        serverInfo.getAccount().setProperty("user", System.getProperty("user"));
        serverInfo.getAccount().setProperty("password", System.getProperty("password"));
        factory = ADaoFactory.getFactory();

        connect = ConnectionFactory.getInstance();
        connect.setServerInfo(serverInfo);
        connect.open();

        DatabaseMetaData metaData = connect.get().getMetaData();
        if (metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
            connect.get().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        connect.rollback();
        connect.close();
    }

    public static List<Card> generateCards(Predefined predefined, int numberOf) {
        Card[] cards = new Card[numberOf];
        for (int i = 0, id = 1; i < numberOf; i++, id++) {
            Card card = new Card();
            int nextCardId = predefined.nextCardId();
            card.setId(nextCardId);
            card.setWordId(id);
            card.setDictionaryId(id);
            card.setRating(10 * i);
            card.setStatus(CardStatus.elementAt(i));
            for (int definitionIndex = 0; definitionIndex < GramUnit.count(); definitionIndex++) {
                card.add(prepareDefinition(predefined, card.getId(), definitionIndex));
            }
            cards[i] = card;
        }
        return Arrays.asList(cards);
    }

    private static Definition prepareDefinition(Predefined predefined, int cardId, int definitionIndex) {
        Definition definition = new Definition();
        int nextDefinitionId = predefined.nextDefinitionId();
        definition.setId(nextDefinitionId);
        String d = "definition_" + nextDefinitionId;
        definition.setValue(d);
        definition.setGramUnit(GramUnit.elementAt(definitionIndex));
        definition.setCardId(cardId);
        for (int meaningIndex = 0; meaningIndex < GramUnit.count(); meaningIndex++) {
            definition.add(prepareMeaning(predefined, definition.getId()));
        }
        return definition;
    }

    private static Meaning prepareMeaning(Predefined predefined, int definitionId) {
        Meaning meaning = new Meaning();
        int nextMeaningId = predefined.nextMeaningId();
        meaning.setId(nextMeaningId);
        String t = "translation_" + nextMeaningId;
        String s = "synonym_" + nextMeaningId;
        String a = "antonym_" + nextMeaningId;
        String e = "example_" + nextMeaningId;
        meaning.setTranslation(t);
        meaning.setSynonym(s);
        meaning.setAntonym(a);
        meaning.setExample(e);
        meaning.setDefinitionId(definitionId);
        return meaning;
    }

    static class Predefined {

        int cardsNumber;
        int definitionsNumber;
        int meaningNumber;

        private int cardTestIndex;
        private int definitionTestIndex;
        private int meaningTestIndex;

        Predefined() {
        }

        Predefined(int cardsNumber, int definitionsNumber, int meaningNumber) {
            this.cardsNumber = cardsNumber;
            this.definitionsNumber = definitionsNumber;
            this.meaningNumber = meaningNumber;
        }

        public int nextCardId() {
            cardTestIndex++;
            return cardsNumber + cardTestIndex;
        }

        public int nextDefinitionId() {
            definitionTestIndex++;
            return definitionsNumber + definitionTestIndex;
        }

        public int nextMeaningId() {
            meaningTestIndex++;
            return meaningNumber + meaningTestIndex;
        }
    }

}