package get.wordy.dao.impl;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.CardStatus;
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
        newCard.setWordId(3);
        newCard.setDictionaryId(2);
        newCard.setScore(10);
        newCard.setStatus(CardStatus.POSTPONED);
        newCard.setInsertedAt(Instant.now());

        // insert
        cardDao.insert(newCard);

        // assert
        List<Card> cards = cardDao.selectCardsForDictionary(2);
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals(2, cards.getFirst().getId());
        Card actual = cards.getLast();
        assertNotNull(actual);
        assertTrue(actual.getId() >= EXPECTED_NEW_ID);
        assertEquals(3, actual.getWordId());
        assertEquals(2, actual.getDictionaryId());
        assertEquals(CardStatus.POSTPONED, actual.getStatus());
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
        updatedCard.setInsertedAt(Instant.now());

        int i = cardDao.update(updatedCard);
        assertEquals(1, i);

        assertCards(2);
    }

    @Test
    public void testDelete() throws DaoException {
        cardDao.delete(2);
        // update list
        List<Card> cards = cardDao.selectCardsForDictionary(2);
        assertNotNull(cards);
        assertEquals(0, cards.size());
    }

    @Test
    public void testLoadCardHeadline() throws DaoException {
        Card first = cardDao.selectById(1);
        assertNotNull(first);
        assertEquals(1, first.getId());
        assertEquals(1, first.getWordId());
        assertEquals(1, first.getDictionaryId());
        assertEquals(CardStatus.EDIT, first.getStatus());

        Card second = cardDao.selectById(2);
        assertNotNull(second);
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

        List<Card> cards = cardDao.selectCardsForDictionary(1);
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
            Collection<Card> cards = cardDao.selectCardsForDictionary(id);
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

    private void assertCards(int dictionaryId) throws DaoException {
        List<Card> cards = cardDao.selectCardsForDictionary(dictionaryId);
        assertNotNull(cards);
        assertEquals(1, cards.size());
        Card actual = cards.getFirst();
        assertNotNull(actual);
        assertEquals(2, actual.getId());
        assertEquals(2, actual.getWordId());
        assertEquals(2, actual.getDictionaryId());
        assertEquals(CardStatus.LEARNT, actual.getStatus());
    }

    @Test
    public void testGenerateCards() throws DaoException {
        int dictionaryId = 2;
        Set<Integer> wordIds = Set.of(3); // abandoned word IDs
        cardDao.addNewCards(dictionaryId, wordIds);
        // assert
        List<Card> cards = cardDao.selectCardsForDictionary(dictionaryId);
        assertNotNull(cards);
        assertEquals(2, cards.size());

        Card first = cards.getFirst();
        assertEquals(2, first.getId());

        Card actual = cards.getLast();
        assertTrue(actual.getId() >= EXPECTED_NEW_ID);
        assertTrue(first.getInsertedAt().isBefore(actual.getInsertedAt())); // oldest first
    }

    @Test
    void updateStatus() throws DaoException {
        int i = cardDao.updateStatus(2, CardStatus.LEARNT, 100);
        assertEquals(1, i);
        assertCards(2);
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
        Card actual = cardDao.selectCardsForDictionary(1)
                .getFirst();
        assertEquals(1, actual.getId());
        assertEquals(CardStatus.TO_LEARN, actual.getStatus());
        assertEquals(25, actual.getScore());
        actual = cardDao.selectCardsForDictionary(2)
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

}