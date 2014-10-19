package get.wordy.core.api.dao;

import get.wordy.core.bean.Dictionary;

import java.util.List;

public interface IDictionaryDao {

    public void insert(Dictionary dictionary) throws DaoException;

    public void delete(Dictionary dictionary) throws DaoException;

    public void update(Dictionary dictionary) throws DaoException;

    public List<Dictionary> selectAll() throws DaoException;

    public int count() throws DaoException;

}