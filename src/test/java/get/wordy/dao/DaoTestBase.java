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

    public static List<Card> getCards(Predefined predefined, int numberToGenerate) {
        int size = predefined.getCardsNumber();
        Card[] cards = new Card[numberToGenerate];
        for (int i = 0, id = 1; i < numberToGenerate; i++, id++) {
            Card card = new Card();
            card.setId(id + size);
            card.setWordId(id);
            card.setDictionaryId(id);
            card.setRating(10 * i);
            card.setStatus(CardStatus.elementAt(i));
            for (int definitionIndex = 1; definitionIndex <= GramUnit.count() + 1; definitionIndex++) {
                card.add(prepareDefinition(i, card.getId(), predefined, definitionIndex));
            }
            cards[i] = card;
        }
        return Arrays.asList(cards);
    }

    private static Definition prepareDefinition(int i, int cardId, Predefined predefined, int index) {
        int c = 97 + i;
        String d = "definition_" + String.valueOf((char) c);
        Definition definition = new Definition();
        definition.setId(predefined.getDefinitionsNumber() + (i * (GramUnit.count() + 1)) + index);
        definition.setValue(d);
        definition.setGramUnit(GramUnit.elementAt(index));
        definition.setCardId(cardId);
        for (int meaningIndex = 1; meaningIndex <= GramUnit.count() + 1; meaningIndex++) {
            definition.add(prepareMeaning(i, definition.getId(), predefined, meaningIndex));
        }
        return definition;
    }

    private static Meaning prepareMeaning(int i, int definitionId, Predefined predefined, int index) {
        int c = 97 + i;
        String add = "meaning_";
        String t = add + String.valueOf((char) (c++));
        String s = add + String.valueOf((char) (c++));
        String a = add + String.valueOf((char) (c++));
        String e = add + String.valueOf((char) (c++));

        Meaning meaning = new Meaning();
        int v = GramUnit.count() + 1;
        meaning.setId(predefined.getMeaningNumber() + (i * v) + index * v);
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

        Predefined(int cardsNumber, int definitionsNumber, int meaningNumber) {
            this.cardsNumber = cardsNumber;
            this.definitionsNumber = definitionsNumber;
            this.meaningNumber = meaningNumber;
        }

        public int getCardsNumber() {
            return cardsNumber;
        }

        public int getDefinitionsNumber() {
            return definitionsNumber;
        }

        public int getMeaningNumber() {
            return meaningNumber;
        }
    }

}