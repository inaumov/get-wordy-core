package get.wordy.core.bean;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class Parent<BEAN extends Cloneable> implements Cloneable {

    LinkedHashSet<BEAN> children = new LinkedHashSet<>();

    List<BEAN> getChildren() {
        return new ArrayList<>(children);
    }

    public void add(BEAN bean) {
        children.add(bean);
    }

    public void addAll(List<BEAN> beans) {
        this.children.addAll(beans);
    }

    @Override
    protected Parent clone() throws CloneNotSupportedException {
        Parent clone = (Parent) super.clone();
        clone.children = (LinkedHashSet) this.children.clone();
        return clone;
    }

}