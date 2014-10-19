package get.wordy.core.api.dao;

import get.wordy.core.api.IScore;
import get.wordy.core.bean.Card;
import get.wordy.core.bean.Definition;
import get.wordy.core.bean.Dictionary;
import get.wordy.core.bean.wrapper.CardStatus;

import java.util.List;

public interface ICardDao {

    public void insert(Card card) throws DaoException;

    public void delete(Card card) throws DaoException;

    public void update(Card card) throws DaoException;

    public List<Card> selectCardsForDictionary(Dictionary dictionary) throws DaoException;

    public List<Card> selectAllCardsSortedBy(String fieldName) throws DaoException;

    public int[] selectCardsForExercise(Dictionary dictionary, int limit) throws DaoException;

    public List<Definition> getDefinitionsFor(Card card) throws DaoException;

    public IScore getScore(Dictionary dictionary) throws DaoException;

    public void resetScore(Dictionary dictionary) throws DaoException;

}