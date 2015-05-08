package get.wordy.core.api.dao;

import get.wordy.core.bean.Word;

import java.util.List;
import java.util.Set;

public interface IWordDao {

    public void insert(Word word) throws DaoException;

    public Set<Integer> generate(Set<String> words) throws DaoException;

    public void delete(Word word) throws DaoException;

    public void update(Word word) throws DaoException;

    public List<Word> selectAll() throws DaoException;

}