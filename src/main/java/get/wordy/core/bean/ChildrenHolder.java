package get.wordy.core.bean;

import java.util.ArrayList;
import java.util.List;

public abstract class ChildrenHolder<T extends Cloneable> implements Cloneable {

    private ArrayList<T> childrenList = new ArrayList<>();

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

    @Override
    protected ChildrenHolder clone() throws CloneNotSupportedException {
        ChildrenHolder clone = (ChildrenHolder) super.clone();
        clone.childrenList = (ArrayList) this.childrenList.clone();
        return clone;
    }

}