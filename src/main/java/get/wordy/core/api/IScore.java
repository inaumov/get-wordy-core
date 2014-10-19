package get.wordy.core.api;

public interface IScore {

    int getEditCnt();

    int getPostponedCnt();

    int getToLearnCnt();

    int getLearntCnt();

    int getTotalCount();

}