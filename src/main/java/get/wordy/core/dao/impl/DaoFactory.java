package get.wordy.core.dao.impl;

import get.wordy.core.db.ConnectionWrapper;

public class DaoFactory {

    protected final ConnectionWrapper connectionWrapper = ConnectionWrapper.getInstance();

    public static DaoFactory getFactory() {
        return new DaoFactory();
    }

    public DictionaryDao getDictionaryDao() {
        return new DictionaryDao(connectionWrapper);
    }

    public WordDao getWordDao() {
        return new WordDao(connectionWrapper);
    }

    public CardDao getCardDao() {
        return new CardDao(connectionWrapper);
    }

}