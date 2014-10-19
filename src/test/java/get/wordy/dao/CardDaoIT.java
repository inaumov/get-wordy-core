package get.wordy.dao;

import get.wordy.core.api.IScore;
import get.wordy.core.api.dao.DaoException;
import get.wordy.core.api.dao.ICardDao;
import get.wordy.core.api.db.ColumnNames;
import get.wordy.core.bean.Card;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.Meaning;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.bean.wrapper.GramUnit;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CardDaoIT extends DaoTestBase {

    private static final int PREDEFINED_CARDS_CNT = 5;
    private static final int PREDEFINED_DEFINITIONS_CNT = 5;
    private static final int PREDEFINED_MEANINGS_CNT = 5;
    private static final int DEFAULT_RATING = 50;

    private ICardDao cardDao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        cardDao = factory.getCardDao();
        assertNotNull(cardDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Iterator<Card> iterator = getTestCardsIterator();
        while (iterator.hasNext()) {
            Card expected = iterator.next();
            // create a new card
            Card newCard = new Card();
            newCard.setWordId(expected.getWordId());
            newCard.setDictionaryId(expected.getDictionaryId());
            newCard.setStatus(expected.getStatus());

            for (Definition definition : expected.getDefinitions()) {
                // create a new definition
                Definition newDefinition = new Definition();
                newDefinition.setValue(definition.getValue());
                newDefinition.setGramUnit(definition.getGramUnit());
                newDefinition.setCardId(definition.getCardId());
                for (Meaning meaning : definition.getMeanings()) {
                    // create new meaning
                    Meaning newMeaning = new Meaning();
                    newMeaning.setDefinitionId(meaning.getDefinitionId());
                    newMeaning.setTranslation(meaning.getTranslation());
                    newMeaning.setSynonym(meaning.getSynonym());
                    newMeaning.setAntonym(meaning.getAntonym());
                    newMeaning.setExample(meaning.getExample());
                    newDefinition.add(newMeaning);
                }
                newCard.add(newDefinition);
            }
            // insert
            cardDao.insert(newCard);
            // assert
            List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(expected.getDictionaryId()));
            assertNotNull(cards);
            assertEquals(2, cards.size());
            assertCard(expected, cards.get(1));
        }
    }

    @Test
    public void testUpdate() throws DaoException {
        Iterator<Card> iterator = getTestCardsIterator();
        while (iterator.hasNext()) {
            Card expected = iterator.next();
            Card updatedCard = new Card();
            updatedCard.setId(5); // the last
            updatedCard.setWordId(expected.getWordId());
            updatedCard.setDictionaryId(expected.getDictionaryId());
            updatedCard.setStatus(expected.getStatus());
            updatedCard.setRating(expected.getRating());
            cardDao.update(updatedCard);

            assertPredefinedCards(cardDao, true);
            List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(expected.getDictionaryId()));
            Card card;
            switch (expected.getDictionaryId()) {
                case 5: {
                    assertEquals(1, cards.size());
                    card = cards.iterator().next();
                    assertNotNull(card);
                    break;
                }
                default: {
                    assertEquals(2, cards.size());
                    card = cards.get(1);
                    assertNotNull(card);
                }
            }
            assertEquals(updatedCard.getId(), card.getId());
            assertEquals(updatedCard.getWordId(), card.getWordId());
            assertEquals(updatedCard.getDictionaryId(), card.getDictionaryId());
            assertEquals(updatedCard.getStatus(), card.getStatus());
        }
    }

    @Test
    public void testDelete() throws DaoException {
        for (int i = 0, id = 1; i < PREDEFINED_CARDS_CNT; i++, id++) {
            List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(id));
            assertNotNull(cards);
            assertEquals(1, cards.size());
            // remove first
            Card toRemove = cards.iterator().next();
            cardDao.delete(toRemove);
            // update list
            cards = cardDao.selectCardsForDictionary(getDictionary(id));
            assertNotNull(cards);
            assertEquals(0, cards.size());
        }
    }

    @Test
    public void testSelectAllFor() throws DaoException {
        assertPredefinedCards(cardDao);
    }

    @Test
    public void testSelectAllSortedByInsertTime() throws DaoException {
        Collection<Card> cards = cardDao.selectAllCardsSortedBy(ColumnNames.CREATE_TIME);
        assertNotNull(cards);
        assertFalse(cards.isEmpty());

        Object[] cardsArr = cards.toArray();
        Card prev = (Card) cardsArr[0];
        for (int i = 1, id = 5; i < PREDEFINED_CARDS_CNT; i++, id--) {
            assertEquals(id, prev.getId());
            assertEquals(id, prev.getWordId());
            assertEquals(id, prev.getDictionaryId());
            assertEquals(CardStatus.EDIT, prev.getStatus());

            Card next = (Card) cardsArr[i];
            assertTrue(next.getInsertTime().after(prev.getInsertTime()));
            prev = next;
        }
        assertEquals(1, prev.getId());
        assertEquals(1, prev.getWordId());
        assertEquals(1, prev.getDictionaryId());
        assertEquals(CardStatus.EDIT, prev.getStatus());
    }

    @Test
    public void testSelectForExercise() throws DaoException {
        // prepare
        for (int i = 0, id = 1; i < PREDEFINED_CARDS_CNT; i++, id++) {
            List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(id));
            Card next = cards.iterator().next();
            next.setDictionaryId(1);
            next.setStatus(CardStatus.TO_LEARN);
            cardDao.update(next);
        }

        // test
        int[] ids = cardDao.selectCardsForExercise(getDictionary(1), PREDEFINED_CARDS_CNT);
        assertNotNull(ids);
        assertEquals(PREDEFINED_CARDS_CNT, ids.length);

        List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(1));
        assertEquals(PREDEFINED_CARDS_CNT, cards.size());
        assertStatus(cards, CardStatus.TO_LEARN);

    }

    @Test
    public void testShowStatistic() throws DaoException {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(1);

        IScore score = cardDao.getScore(dictionary);
        assertEquals(1, score.getTotalCount());
        assertEquals(1, score.getEditCnt());
        assertEquals(0, score.getToLearnCnt());
        assertEquals(0, score.getPostponedCnt());
        assertEquals(0, score.getLearntCnt());
    }

    @Test
    public void testResetStatistics() throws DaoException {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(1);
        cardDao.resetScore(dictionary);

        List<Card> cards = cardDao.selectCardsForDictionary(dictionary);
        assertStatus(cards, CardStatus.DEFAULT_STATUS);
    }

    @Test
    public void testSelectAllForDictionary() throws DaoException {
        Iterator<Card> iterator = getTestCardsIterator();
        while (iterator.hasNext()) {
            Card expected = iterator.next();
            // check names of all dictionaries before insertion
            Dictionary dictionary = new Dictionary();
            dictionary.setId(expected.getDictionaryId());
            Collection<Card> cards = cardDao.selectCardsForDictionary(dictionary);
            assertNotNull(cards);
            assertEquals(1, cards.size());

            Iterator<Card> it = cards.iterator();
            while (it.hasNext()) {
                Card next = it.next();
                assertEquals(expected.getWordId(), next.getWordId());
                assertEquals(expected.getDictionaryId(), next.getDictionaryId());
                assertEquals(CardStatus.DEFAULT_STATUS, next.getStatus());
                assertEquals(50, next.getRating());
            }
        }
    }

    private Iterator<Card> getTestCardsIterator() {
        Predefined predefined = new Predefined(PREDEFINED_CARDS_CNT, PREDEFINED_DEFINITIONS_CNT, PREDEFINED_MEANINGS_CNT);
        return getCards(predefined, 5).iterator();
    }

    private static Dictionary getDictionary(int dictionaryId) {
        return new Dictionary(dictionaryId, "");
    }

    private static void assertPredefinedCards(ICardDao cardDao) throws DaoException {
        assertPredefinedCards(cardDao, false);
    }

    private static void assertPredefinedCards(ICardDao cardDao, boolean skipLast) throws DaoException {
        int cnt = skipLast ? PREDEFINED_CARDS_CNT - 1 : PREDEFINED_CARDS_CNT;
        for (int id = 1; id <= cnt; id++) {
            Card card = cardDao.selectCardsForDictionary(getDictionary(id)).iterator().next();
            assertNotNull(card);
            assertEquals(id, card.getId());
            assertEquals(id, card.getWordId());
            assertEquals(id, card.getDictionaryId());
            assertEquals(CardStatus.EDIT, card.getStatus());
            for (int definitionId = 1; definitionId <= card.getDefinitions().size(); definitionId++) {
                Definition definition = card.getDefinitions().get(definitionId - 1);
                assertEquals(definitionId, definition.getId());
                assertEquals("definition" + definitionId, definition.getValue());
                assertEquals(GramUnit.NOUN, definition.getGramUnit());
                assertEquals(id, definition.getCardId());
                for (int meaningId = 1; meaningId <= definition.getMeanings().size(); meaningId++) {
                    Meaning meaning = definition.getMeanings().get(meaningId - 1);
                    assertEquals(meaningId, meaning.getId());
                    assertEquals("translation" + meaningId, meaning.getTranslation());
                    assertEquals("synonym" + meaningId, meaning.getSynonym());
                    assertEquals("antonym" + meaningId, meaning.getAntonym());
                    assertEquals("example" + meaningId, meaning.getExample());
                    assertEquals(meaningId, meaning.getDefinitionId());
                }
            }
        }
    }

    private static void assertCard(Card expected, Card actual) throws DaoException {
        assertNotNull(actual);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getWordId(), actual.getWordId());
        assertEquals(expected.getDictionaryId(), actual.getDictionaryId());
        assertEquals(expected.getStatus(), actual.getStatus());

        assertDefinitions(expected.getDefinitions(), expected.getDefinitions());
    }

    private static void assertDefinitions(List<Definition> expected, List<Definition> actual) throws DaoException {
        assertNotNull(actual);
        assertEquals(GramUnit.count() + 1, actual.size());

        for (int definitionId = 1; definitionId <= expected.size(); definitionId++) {
            Definition expectedDefinition = expected.get(definitionId - 1);
            Definition actualDefinition = actual.get(definitionId - 1);
            assertEquals(expectedDefinition.getId(), actualDefinition.getId());
            assertEquals(expectedDefinition.getValue(), actualDefinition.getValue());
            assertEquals(expectedDefinition.getGramUnit(), actualDefinition.getGramUnit());
            assertEquals(expectedDefinition.getCardId(), actualDefinition.getCardId());

            assertMeanings(expectedDefinition.getMeanings(), actualDefinition.getMeanings());
        }
    }

    private static void assertMeanings(List<Meaning> expected, List<Meaning> actual) throws DaoException {
        assertNotNull(actual);
        assertEquals(GramUnit.count() + 1, actual.size());

        for (int meaningId = 1; meaningId <= expected.size(); meaningId++) {
            Meaning expectedMeaning = expected.get(meaningId - 1);
            Meaning actualMeaning = actual.get(meaningId - 1);
            assertEquals(expectedMeaning.getId(), actualMeaning.getId());
            assertEquals(expectedMeaning.getTranslation(), actualMeaning.getTranslation());
            assertEquals(expectedMeaning.getSynonym(), actualMeaning.getSynonym());
            assertEquals(expectedMeaning.getAntonym(), actualMeaning.getAntonym());
            assertEquals(expectedMeaning.getExample(), actualMeaning.getExample());
            assertEquals(expectedMeaning.getDefinitionId(), actualMeaning.getDefinitionId());
        }
    }


    private static void assertStatus(List<Card> cards, CardStatus status) {
        for (Card card : cards) {
            assertSame(status, card.getStatus());
            if (status == CardStatus.LEARNT) {
                assertEquals(100, card.getRating());
            } else if (status == CardStatus.EDIT) {
                assertEquals(0, card.getRating());
            } else {
                assertEquals(DEFAULT_RATING, card.getRating());
            }
        }
    }

}