package get.wordy.core.dao.impl;

import get.wordy.core.db.LocalTxManager;

public class DaoFactory {

    protected final LocalTxManager txManager = LocalTxManager.getInstance();

    public static DaoFactory getFactory() {
        return new DaoFactory();
    }

    public DictionaryDao getDictionaryDao() {
        return new DictionaryDao(txManager);
    }

    public WordDao getWordDao() {
        return new WordDao(txManager);
    }

    public CardDao getCardDao() {
        return new CardDao(txManager);
    }

}