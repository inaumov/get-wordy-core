package get.wordy.core.dao.impl;

import get.wordy.core.api.dao.*;
import get.wordy.core.dao.ADaoFactory;

public class DaoFactory extends ADaoFactory {

    @Override
    public IDictionaryDao getDictionaryDao() {
        return new DictionaryDao(connectionFactory);
    }

    @Override
    public IWordDao getWordDao() {
        return new WordDao(connectionFactory);
    }

    @Override
    public ICardDao getCardDao() {
        return new CardDao(connectionFactory);
    }

}