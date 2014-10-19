package get.wordy.core.dao;

import get.wordy.core.api.dao.*;
import get.wordy.core.api.db.IConnectionFactory;
import get.wordy.core.dao.impl.DaoFactory;
import get.wordy.core.db.ConnectionFactory;

public abstract class ADaoFactory {

    protected final IConnectionFactory connectionFactory = ConnectionFactory.getInstance();

    public abstract IDictionaryDao getDictionaryDao();

    public abstract IWordDao getWordDao();

    public abstract ICardDao getCardDao();

    public static ADaoFactory getFactory() {
        return new DaoFactory();
    }

}