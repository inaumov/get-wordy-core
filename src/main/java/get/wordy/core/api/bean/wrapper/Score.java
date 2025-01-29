package get.wordy.core.api.bean.wrapper;

import get.wordy.core.api.bean.CardStatus;

public class Score {

    private int deferredCnt, toLearnCnt, learntCnt;

    public Score(int deferred, int toLearn, int learnt) {
        this.setDeferredCnt(deferred);
        this.setToLearnCnt(toLearn);
        this.setLearntCnt(learnt);
    }

    public Score() {
    }

    public void setDeferredCnt(int deferredCnt) {
        this.deferredCnt = deferredCnt;
    }

    public int getDeferredCnt() {
        return deferredCnt;
    }

    public void setToLearnCnt(int toLearnCnt) {
        this.toLearnCnt = toLearnCnt;
    }

    public int getToLearnCnt() {
        return toLearnCnt;
    }

    public void setLearntCnt(int learntCnt) {
        this.learntCnt = learntCnt;
    }

    public int getLearntCnt() {
        return learntCnt;
    }

    public int getTotalCount() {
        return deferredCnt + toLearnCnt + learntCnt;
    }

    public void withScoreCount(CardStatus cardStatus, int count) {
        switch (cardStatus) {
            case LEARNT -> this.setLearntCnt(count);
            case TO_LEARN -> this.setToLearnCnt(count);
            case DEFERRED -> this.setDeferredCnt(count);
        }
    }

}