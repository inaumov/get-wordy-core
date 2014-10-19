package get.wordy.core.wrapper;

import get.wordy.core.api.IScore;
import get.wordy.core.bean.wrapper.CardStatus;

public final class Score implements IScore {

    private int editCnt, postponedCnt, toLearnCnt, learntCnt;

    public Score(int edit, int postponed, int toLearn, int learnt) {
        this.setEditCnt(edit);
        this.setPostponedCnt(postponed);
        this.setToLearnCnt(toLearn);
        this.setLearntCnt(learnt);
    }

    public Score() {
        ;
    }

    public void setEditCnt(int editCnt) {
        this.editCnt = editCnt;
    }

    @Override
    public int getEditCnt() {
        return editCnt;
    }

    public void setPostponedCnt(int postponedCnt) {
        this.postponedCnt = postponedCnt;
    }

    @Override
    public int getPostponedCnt() {
        return postponedCnt;
    }

    public void setToLearnCnt(int toLearnCnt) {
        this.toLearnCnt = toLearnCnt;
    }

    @Override
    public int getToLearnCnt() {
        return toLearnCnt;
    }

    public void setLearntCnt(int learntCnt) {
        this.learntCnt = learntCnt;
    }

    @Override
    public int getLearntCnt() {
        return learntCnt;
    }

    @Override
    public int getTotalCount() {
        return editCnt + postponedCnt + toLearnCnt + learntCnt;
    }

    public void setScoreCount(CardStatus cardStatus, int count) {

        switch (cardStatus) {

            case LEARNT: {
                this.setLearntCnt(count);
                break;
            }
            case TO_LEARN: {
                this.setToLearnCnt(count);
                break;
            }
            case EDIT: {
                this.setEditCnt(count);
                break;
            }
            case POSTPONED: {
                this.setPostponedCnt(count);
                break;
            }
        }
    }

}