package get.wordy.dao.impl;

import get.wordy.core.api.bean.Progress;
import get.wordy.core.api.bean.CardStatus;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.ProgressDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProgressDaoTest extends BaseDaoTest {

    private ProgressDao progressDao;
    private final OwnerId ownerId = new OwnerId("john-123", "user");

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        progressDao = daoFactory.getCardDao();
        assertNotNull(progressDao);
    }

    @Test
    public void testInsert() throws DaoException {
        Progress newCard = new Progress();
        newCard.setWordId(3);
        newCard.setVocabId(2);
        newCard.setScore(0);
        newCard.setStatus(CardStatus.TO_LEARN);
        newCard.setInsertedAt(Instant.now());

        // insert
        progressDao.insert(ownerId, newCard);

        // assert
        List<Progress> cards = progressDao.selectCards(ownerId, 2, 2, 3);
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals(2, cards.getFirst().getWordId());
        Progress actual = cards.getLast();
        assertNotNull(actual);
        assertEquals(newCard.getWordId(), actual.getWordId());
        assertEquals(3, actual.getWordId());
        assertEquals(2, actual.getVocabId());
        assertEquals(CardStatus.TO_LEARN, actual.getStatus());
    }

    @Test
    public void testDelete() throws DaoException {
        progressDao.delete(ownerId, 2, 2);
        // update list
        List<Progress> cards = progressDao.selectCards(ownerId, 2, 2);
        assertNotNull(cards);
        assertEquals(0, cards.size());
    }

    @Test
    public void testGetCard() throws DaoException {
        Progress first = progressDao.selectById(ownerId, 1, 1);
        assertNotNull(first);
        assertEquals(1, first.getWordId());
        assertEquals(1, first.getVocabId());
        assertEquals(CardStatus.TO_LEARN, first.getStatus());

        Progress second = progressDao.selectById(ownerId, 2, 2);
        assertNotNull(second);
        assertEquals(2, second.getWordId());
        assertEquals(2, second.getVocabId());
        assertEquals(CardStatus.TO_LEARN, second.getStatus());

        assertTrue(first.getInsertedAt().isBefore(second.getInsertedAt())); // oldest first
    }

    @Test
    public void testGetProgressSummary() throws DaoException {
        Map<String, Integer> score = progressDao.getProgressSummary(ownerId, 1);
        assertEquals(1, score.get(CardStatus.TO_LEARN.name()));
    }

    @Test
    public void testResetProgress() throws DaoException {
        Progress card = new Progress();
        card.setVocabId(1);
        card.setWordId(1);
        card.setStatus(CardStatus.TO_LEARN);
        card.setScore(0);
        int rowsAffected = progressDao.updateProgress(ownerId, card);
        assertTrue(rowsAffected > 0);

        List<Progress> cards = progressDao.selectCards(ownerId, 1, 1);
        Progress first = cards.getFirst();
        assertEquals(0, first.getScore());
        assertEquals(CardStatus.TO_LEARN, first.getStatus());
    }

    @Test
    public void testSelectAllByVocabId() throws DaoException {
        Collection<Progress> cards = progressDao.selectCards(ownerId, 1);
        assertNotNull(cards);
        assertEquals(1, cards.size());

        Iterator<Progress> it = cards.iterator();
        Progress card = it.next();
        assertEquals(1, card.getWordId());
        assertEquals(1, card.getVocabId());
        assertEquals(CardStatus.TO_LEARN, card.getStatus());
        assertEquals(3, card.getScore());
    }

    @Test
    public void testAddCards() throws DaoException {
        int vocabId = 2;

        Progress newCard = new Progress();
        newCard.setWordId(3);
        newCard.setVocabId(2);
        newCard.setScore(0);
        newCard.setStatus(CardStatus.TO_LEARN);
        newCard.setInsertedAt(Instant.now());

        progressDao.addCards(ownerId, List.of(newCard));

        // assert
        List<Progress> cards = progressDao.selectCards(ownerId, vocabId, 2, 3);
        assertNotNull(cards);
        assertEquals(2, cards.size());

        Progress first = cards.getFirst();
        assertEquals(vocabId, first.getVocabId());
        assertEquals(2, first.getWordId());

        Progress actual = cards.getLast();
        assertEquals(vocabId, actual.getVocabId());
        assertEquals(3, actual.getWordId());
        assertTrue(first.getInsertedAt().isBefore(actual.getInsertedAt())); // oldest first
    }

    @Test
    void testUpdateProgress() throws DaoException {
        Progress card = new Progress();
        card.setVocabId(2);
        card.setWordId(2);
        card.setScore(100);
        card.setStatus(CardStatus.LEARNT);
        int updatedCnt = progressDao.updateProgress(ownerId, card);
        assertEquals(1, updatedCnt);

        // verify
        List<Progress> cards = progressDao.selectCards(ownerId, 2, 2);
        assertNotNull(cards);
        assertEquals(1, cards.size());
        Progress actual = cards.getFirst();
        assertNotNull(actual);
        assertEquals(2, actual.getWordId());
        assertEquals(2, actual.getVocabId());
        assertEquals(CardStatus.LEARNT, actual.getStatus());
        assertEquals(100, actual.getScore());
    }

    @Test
    void testBatchUpsertProgress() throws DaoException {
        Progress card1 = new Progress();
        card1.setVocabId(1);
        card1.setStatus(CardStatus.DEFERRED);
        card1.setScore(25);
        card1.setWordId(1);
        Progress card2 = new Progress();
        card2.setVocabId(2);
        card2.setStatus(CardStatus.DEFERRED);
        card2.setScore(80);
        card2.setWordId(2);

        progressDao.batchUpsertProgress(ownerId, List.of(card1, card2));

        // assert
        Progress actual = progressDao.selectCards(ownerId, 1, new int[]{1, 2})
                .getFirst();
        assertEquals(1, actual.getWordId());
        assertEquals(CardStatus.DEFERRED, actual.getStatus());
        assertEquals(25, actual.getScore());
        actual = progressDao.selectCards(ownerId, 2, new int[]{1, 2})
                .getFirst();
        assertEquals(2, actual.getWordId());
        assertEquals(CardStatus.DEFERRED, actual.getStatus());
        assertEquals(80, actual.getScore());
    }

}