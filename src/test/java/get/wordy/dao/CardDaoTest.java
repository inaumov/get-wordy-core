package get.wordy.dao;

import get.wordy.core.bean.Card;
import get.wordy.core.bean.Collocation;
import get.wordy.core.bean.Context;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.CardDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class CardDaoTest extends BaseDaoTest {

    private static final int PREDEFINED_CARDS_CNT = 2;
    private static final int EXPECTED_NEW_ID = 3;
    private static final int DEFAULT_SCORE = 50;

    private CardDao cardDao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        cardDao = daoFactory.getCardDao();
        assertNotNull(cardDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Card newCard = new Card();
        int nextCardId = 3;
        newCard.setId(nextCardId);
        newCard.setWordId(3);
        newCard.setDictionaryId(2);
        newCard.setScore(10);
        newCard.setStatus(CardStatus.POSTPONED);
        newCard.addContext(prepareContext(newCard.getWordId(), 3));
        newCard.addCollocation(prepareCollocation(newCard.getWordId(), 3));
        newCard.setInsertedAt(Instant.now());

        // insert
        cardDao.insert(newCard);

        // assert
        List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(2));
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals(2, cards.get(0).getId());
        Card actual = cards.get(1);
        assertNotNull(actual);
        assertTrue(actual.getId() >= EXPECTED_NEW_ID);
        assertEquals(3, actual.getWordId());
        assertEquals(2, actual.getDictionaryId());
        assertEquals(CardStatus.POSTPONED, actual.getStatus());
        assertContexts(newCard.getContexts(), cardDao.getContextsFor(actual));
        assertCollocations(newCard.getCollocations(), cardDao.getCollocationsFor(actual));
    }

    @Test
    public void testUpdate() throws DaoException {
        Card updatedCard = new Card();
        updatedCard.setId(2); // the last
        updatedCard.setWordId(2);
        updatedCard.setDictionaryId(2);
        updatedCard.setStatus(CardStatus.LEARNT);
        updatedCard.setScore(100);
        updatedCard.setUpdatedAt(Instant.now());
        updatedCard.addContext(prepareContext(2, updatedCard.getWordId()));
        updatedCard.addCollocation(prepareCollocation(2, updatedCard.getWordId()));
        updatedCard.setInsertedAt(Instant.now());

        cardDao.update(updatedCard);

        assertCards(getDictionary(2));
    }

    @Test
    public void testDelete() throws DaoException {
        for (int i = 0, id = 1; i < PREDEFINED_CARDS_CNT; i++, id++) {
            List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(id));
            assertNotNull(cards);
            assertEquals(1, cards.size());
            // remove first
            Card toRemove = cards.getFirst();
            cardDao.delete(toRemove);
            // update list
            cards = cardDao.selectCardsForDictionary(getDictionary(id));
            assertNotNull(cards);
            assertEquals(0, cards.size());
        }
    }

    @Test
    public void testSelectAllSortedByInsertTimeNewCardsFirst() throws DaoException {
        List<Card> cards = cardDao.selectAllCardsSortedBy("create_time");
        assertNotNull(cards);
        assertFalse(cards.isEmpty());

        Card first = cards.getFirst();
        assertEquals(1, first.getId());
        assertEquals(1, first.getWordId());
        assertEquals(1, first.getDictionaryId());
        assertEquals(CardStatus.EDIT, first.getStatus());

        Card second = cards.get(1);
        assertEquals(2, second.getId());
        assertEquals(2, second.getWordId());
        assertEquals(2, second.getDictionaryId());
        assertEquals(CardStatus.TO_LEARN, second.getStatus());

        assertTrue(first.getInsertedAt().isBefore(second.getInsertedAt())); // oldest first
    }

    @Test
    public void testSelectCardIdsForExercise() throws DaoException {
        // test no cards
        int[] ids1 = cardDao.selectCardIdsForExercise(1, PREDEFINED_CARDS_CNT);
        assertNotNull(ids1);
        assertEquals(0, ids1.length);
        // test 1 cards to learn
        int[] ids2 = cardDao.selectCardIdsForExercise(2, PREDEFINED_CARDS_CNT);
        assertNotNull(ids2);
        assertEquals(1, ids2.length);
        assertEquals(2, ids2[0]);
    }

    @Test
    public void testShowStatistic() throws DaoException {
        Map<String, Integer> score = cardDao.getScoreSummary(1);
        assertEquals(1, score.get(CardStatus.EDIT.name()));
    }

    @Test
    public void testResetStatistics() throws DaoException {
        cardDao.resetScore(1, CardStatus.DEFAULT_STATUS);

        List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(1));
        assertStatus(cards);
    }

    @Test
    public void testSelectAllForDictionary() throws DaoException {
        IntStream expectedIds = IntStream.of(1, 2);
        EnumSet<CardStatus> expectedStatuses = EnumSet.of(CardStatus.DEFAULT_STATUS, CardStatus.TO_LEARN);

        PrimitiveIterator.OfInt iterator = expectedIds.iterator();
        while (iterator.hasNext()) {
            int id = iterator.nextInt();
            // check names of all dictionaries before insertion
            Dictionary dictionary = new Dictionary();
            dictionary.setId(id);
            Collection<Card> cards = cardDao.selectCardsForDictionary(dictionary);
            assertNotNull(cards);
            assertEquals(1, cards.size());

            Iterator<Card> it = cards.iterator();
            Card card = it.next();
            assertEquals(id, card.getId());
            assertEquals(id, card.getWordId());
            assertEquals(id, card.getDictionaryId());
            assertTrue(expectedStatuses.remove(card.getStatus()));
            assertEquals(50, card.getScore());
        }
        assertTrue(expectedStatuses.isEmpty());
    }

    private static Dictionary getDictionary(int dictionaryId) {
        return new Dictionary(dictionaryId, "", null);
    }

    private void assertCards(Dictionary dictionary) throws DaoException {
        List<Card> cards = cardDao.selectCardsForDictionary(dictionary);
        assertNotNull(cards);
        assertEquals(1, cards.size());
        Card actual = cards.getFirst();
        assertNotNull(actual);
        assertEquals(2, actual.getId());
        assertEquals(2, actual.getWordId());
        assertEquals(2, actual.getDictionaryId());
        assertEquals(CardStatus.LEARNT, actual.getStatus());

        assertContexts(actual.getContexts(), cardDao.getContextsFor(actual));
        assertCollocations(actual.getCollocations(), cardDao.getCollocationsFor(actual));
    }

    private static void assertContexts(List<Context> expectedDefinitions, List<Context> actualContexts) {
        assertNotNull(actualContexts);

        for (int i = 0; i < expectedDefinitions.size(); i++) {
            Context expected = expectedDefinitions.get(i);
            Context actual = actualContexts.get(i);
            assertTrue(expected.getId() <= actual.getId(), "actual: " + actual.getId());
            assertEquals(expected.getExample(), actual.getExample());
            assertEquals(expected.getWordId(), actual.getWordId());
        }
    }

    private static void assertCollocations(List<Collocation> meaningsExpected, List<Collocation> actualCollocations) {
        assertNotNull(actualCollocations);

        for (int i = 0; i < meaningsExpected.size(); i++) {
            Collocation expected = meaningsExpected.get(i);
            Collocation actual = actualCollocations.get(i);
            assertTrue(expected.getId() <= actual.getId(), "actual: " + actual.getId());
            assertEquals(expected.getExample(), actual.getExample());
            assertEquals(expected.getWordId(), actual.getWordId());
        }
    }

    @Test
    public void testGenerateCards() throws DaoException {
        int dictionaryId = 2;
        Set<Integer> wordIds = Set.of(42, 87);
        Set<Integer> ids = cardDao.generateEmptyCards(dictionaryId, wordIds);
        // assert
        assertFalse(ids.isEmpty());
        List<Card> cards = cardDao.selectCardsForDictionary(getDictionary(dictionaryId));
        assertNotNull(cards);
        assertEquals(3, cards.size());
        Iterator<Card> cardIterator = cards.iterator();
        Card first = cardIterator.next();
        assertEquals(2, first.getId());

        while (cardIterator.hasNext()) {
            Card actual = cardIterator.next();
            assertTrue(actual.getId() >= EXPECTED_NEW_ID);
            assertTrue(first.getInsertedAt().isBefore(actual.getInsertedAt())); // oldest first
        }
    }

    @Test
    void updateStatus() throws DaoException {
        cardDao.updateStatus(2, CardStatus.LEARNT, 100);
        assertCards(getDictionary(2));
    }

    @Test
    void batchUpdateScores() throws DaoException {
        Card card1 = new Card();
        card1.setId(1); // the last
        card1.setStatus(CardStatus.TO_LEARN);
        card1.setScore(25);
        Card card2 = new Card();
        card2.setId(2); // the last
        card2.setStatus(CardStatus.TO_LEARN);
        card2.setScore(80);

        cardDao.batchUpdateScores(List.of(card1, card2));

        // assert
        Card actual = cardDao.selectCardsForDictionary(getDictionary(1))
                .getFirst();
        assertEquals(1, actual.getId());
        assertEquals(CardStatus.TO_LEARN, actual.getStatus());
        assertEquals(25, actual.getScore());
        actual = cardDao.selectCardsForDictionary(getDictionary(2))
                .getFirst();
        assertEquals(2, actual.getId());
        assertEquals(CardStatus.TO_LEARN, actual.getStatus());
        assertEquals(80, actual.getScore());
    }

    private static void assertStatus(List<Card> cards) {
        for (Card card : cards) {
            assertSame(CardStatus.DEFAULT_STATUS, card.getStatus());
            if (CardStatus.DEFAULT_STATUS == CardStatus.LEARNT) {
                assertEquals(100, card.getScore());
            } else if (CardStatus.DEFAULT_STATUS == CardStatus.EDIT) {
                assertEquals(0, card.getScore());
            } else {
                assertEquals(DEFAULT_SCORE, card.getScore());
            }
        }
    }

    private Context prepareContext(int wordId, int contextId) {
        String d = "example" + contextId;
        return new Context(contextId, d, wordId);
    }

    private Collocation prepareCollocation(int wordId, int collocationId) {
        String e = "example" + collocationId;
        return new Collocation(collocationId, e, wordId);
    }

}