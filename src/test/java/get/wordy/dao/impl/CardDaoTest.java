package get.wordy.dao.impl;

import get.wordy.core.api.bean.Card;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.api.id.OwnerId;
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

    private CardDao cardDao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        cardDao = daoFactory.getCardDao();
        assertNotNull(cardDao);
    }

    @Test
    public void testInsert() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");

        Card newCard = new Card();
        newCard.setWordId(3);
        newCard.setVocabId(2);
        newCard.setScore(0);
        newCard.setStatus(CardStatus.TO_LEARN);
        newCard.setInsertedAt(Instant.now());

        // insert
        cardDao.insert(ownerId, newCard);

        // assert
        List<Card> cards = cardDao.selectCardsForDictionary(ownerId, 2);
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals(2, cards.getFirst().getId());
        Card actual = cards.getLast();
        assertNotNull(actual);
        assertTrue(actual.getId() >= EXPECTED_NEW_ID);
        assertEquals(3, actual.getWordId());
        assertEquals(2, actual.getVocabId());
        assertEquals(CardStatus.TO_LEARN, actual.getStatus());
    }

    @Test
    public void testDelete() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        cardDao.delete(ownerId, 2);
        // update list
        List<Card> cards = cardDao.selectCardsForDictionary(ownerId, 2);
        assertNotNull(cards);
        assertEquals(0, cards.size());
    }

    @Test
    public void testGetCard() throws DaoException {
        Card first = cardDao.selectById(1);
        assertNotNull(first);
        assertEquals(1, first.getId());
        assertEquals(1, first.getWordId());
        assertEquals(1, first.getVocabId());
        assertEquals(CardStatus.TO_LEARN, first.getStatus());

        Card second = cardDao.selectById(2);
        assertNotNull(second);
        assertEquals(2, second.getId());
        assertEquals(2, second.getWordId());
        assertEquals(2, second.getVocabId());
        assertEquals(CardStatus.TO_LEARN, second.getStatus());

        assertTrue(first.getInsertedAt().isBefore(second.getInsertedAt())); // oldest first
    }

    @Test
    public void testSelectCardIdsForExercise() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        // test no cards
        int[] idsFromVocab1 = cardDao.selectCardIdsForExercise(ownerId, 1, PREDEFINED_CARDS_CNT);
        assertNotNull(idsFromVocab1);
        assertEquals(1, idsFromVocab1.length);
        assertEquals(1, idsFromVocab1[0]); // card id
        // test 1 cards to learn
        int[] idsFromVocab2 = cardDao.selectCardIdsForExercise(ownerId, 2, PREDEFINED_CARDS_CNT);
        assertNotNull(idsFromVocab2);
        assertEquals(1, idsFromVocab2.length);
        assertEquals(2, idsFromVocab2[0]); // card id
    }

    @Test
    public void testShowStatistic() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        Map<String, Integer> score = cardDao.getScoreSummary(ownerId, 1);
        assertEquals(1, score.get(CardStatus.TO_LEARN.name()));
    }

    @Test
    public void testResetScore() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");

        int rowsAffected = cardDao.updateScore(1, 0);
        assertTrue(rowsAffected > 0);
        rowsAffected = cardDao.updateStatus(1, CardStatus.DEFAULT_STATUS);
        assertTrue(rowsAffected > 0);

        List<Card> cards = cardDao.selectCardsForDictionary(ownerId, 1);
        Card first = cards.getFirst();
        assertEquals(0, first.getScore());
        assertEquals(CardStatus.TO_LEARN, first.getStatus());
    }

    @Test
    public void testSelectAllForDictionary() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        IntStream expectedIds = IntStream.of(1, 2);

        PrimitiveIterator.OfInt iterator = expectedIds.iterator();
        while (iterator.hasNext()) {
            int id = iterator.nextInt();
            // check names of all vocabularies before insertion
            Collection<Card> cards = cardDao.selectCardsForDictionary(ownerId, id);
            assertNotNull(cards);
            assertEquals(1, cards.size());

            Iterator<Card> it = cards.iterator();
            Card card = it.next();
            assertEquals(id, card.getId());
            assertEquals(id, card.getWordId());
            assertEquals(id, card.getVocabId());
            assertEquals(CardStatus.DEFAULT_STATUS, card.getStatus());
            assertEquals(50, card.getScore());
        }
    }

    @Test
    public void testAddCards() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        int dictionaryId = 2;
        Set<Integer> wordIds = Set.of(3); // new words Refs
        cardDao.addCards(ownerId, dictionaryId, wordIds);

        // assert
        List<Card> cards = cardDao.selectCardsForDictionary(ownerId, dictionaryId);
        assertNotNull(cards);
        assertEquals(2, cards.size());

        Card first = cards.getFirst();
        assertEquals(2, first.getId());
        assertEquals(dictionaryId, first.getVocabId());
        assertEquals(2, first.getWordId());

        Card actual = cards.getLast();
        assertTrue(actual.getId() >= EXPECTED_NEW_ID);
        assertEquals(dictionaryId, actual.getVocabId());
        assertEquals(3, actual.getWordId());
        assertTrue(first.getInsertedAt().isBefore(actual.getInsertedAt())); // oldest first
    }

    @Test
    void updateStatus() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        int updatedCnt = cardDao.updateStatus(2, CardStatus.LEARNT);
        assertEquals(1, updatedCnt);

        // verify
        List<Card> cards = cardDao.selectCardsForDictionary(ownerId, 2);
        assertNotNull(cards);
        assertEquals(1, cards.size());
        Card actual = cards.getFirst();
        assertNotNull(actual);
        assertEquals(2, actual.getId());
        assertEquals(2, actual.getWordId());
        assertEquals(2, actual.getVocabId());
        assertEquals(CardStatus.LEARNT, actual.getStatus());
    }

    @Test
    void batchUpdateScores() throws DaoException {
        OwnerId ownerId = new OwnerId("user123", "user");
        Card card1 = new Card();
        card1.setId(1); // the last
        card1.setStatus(CardStatus.POSTPONED);
        card1.setScore(25);
        Card card2 = new Card();
        card2.setId(2); // the last
        card2.setStatus(CardStatus.POSTPONED);
        card2.setScore(80);

        cardDao.batchUpdateScores(List.of(card1, card2));
        cardDao.batchUpdateStatuses(List.of(card1, card2));

        // assert
        Card actual = cardDao.selectCardsForDictionary(ownerId, 1)
                .getFirst();
        assertEquals(1, actual.getId());
        assertEquals(1, actual.getWordId());
        assertEquals(CardStatus.POSTPONED, actual.getStatus());
        assertEquals(25, actual.getScore());
        actual = cardDao.selectCardsForDictionary(ownerId, 2)
                .getFirst();
        assertEquals(2, actual.getId());
        assertEquals(2, actual.getWordId());
        assertEquals(CardStatus.POSTPONED, actual.getStatus());
        assertEquals(80, actual.getScore());
    }

}