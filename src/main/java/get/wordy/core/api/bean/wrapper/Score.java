package get.wordy.core.api.bean.wrapper;

import get.wordy.core.api.bean.CardStatus;

public class Score {

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

    public int getEditCnt() {
        return editCnt;
    }

    public void setPostponedCnt(int postponedCnt) {
        this.postponedCnt = postponedCnt;
    }

    public int getPostponedCnt() {
        return postponedCnt;
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
        return editCnt + postponedCnt + toLearnCnt + learntCnt;
    }

    public void setScoreCount(CardStatus cardStatus, int count) {
        switch (cardStatus) {
            case LEARNT -> this.setLearntCnt(count);
            case TO_LEARN -> this.setToLearnCnt(count);
            case EDIT -> this.setEditCnt(count);
            case POSTPONED -> this.setPostponedCnt(count);
        }
    }

}