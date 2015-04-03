package get.wordy.core.bean;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class ChildrenHolder<T extends Cloneable> implements Cloneable {

    LinkedHashSet<T> childrenList = new LinkedHashSet<>();

    public void add(T child) {
        childrenList.add(child);
    }

    public void addAll(List<T> children) {
        childrenList.addAll(children);
    }

    public List<T> getChildren() {
        return new ArrayList<>(childrenList);
    }

    public void clearDefinition() {
        childrenList.clear();
    }

    @Override
    protected ChildrenHolder clone() throws CloneNotSupportedException {
        ChildrenHolder clone = (ChildrenHolder) super.clone();
        clone.childrenList = (LinkedHashSet) this.childrenList.clone();
        return clone;
    }

}