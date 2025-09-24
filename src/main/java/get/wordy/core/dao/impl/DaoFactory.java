package get.wordy.core.dao.impl;

import get.wordy.core.db.LocalTxManager;

public class DaoFactory {

    private final LocalTxManager txManager;

    private DaoFactory(LocalTxManager txManager) {
        this.txManager = txManager;
    }

    public static DaoFactory withTxManager(LocalTxManager txManager) {
        return new DaoFactory(txManager);
    }

    public ProgressDao getCardDao() {
        return new ProgressDao(txManager);
    }

}