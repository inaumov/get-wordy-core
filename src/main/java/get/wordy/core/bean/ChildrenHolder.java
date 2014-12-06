package get.wordy.core.bean;

import java.util.ArrayList;
import java.util.List;

public abstract class ChildrenHolder<T> {

    private List<T> childrenList = new ArrayList<>();

    public void add(T child) {
        childrenList.add(child);
    }

    // TODO check IfNotExists
    public void addAll(List<T> children) {
        childrenList.addAll(children);
    }

    public List<T> getChildren() {
        return childrenList;
    }

}