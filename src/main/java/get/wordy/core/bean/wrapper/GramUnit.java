package get.wordy.core.bean.wrapper;

import java.util.ArrayList;
import java.util.List;

public enum GramUnit {

    NOUN(0),
    ADJECTIVE(1),
    VERB(2),
    ADVERB(3),
    PHRASAL_VERB(4),
    PHRASE(5),
    IDIOM(6),
    OTHER(7);

    public static final GramUnit DEFAULT_UNIT = GramUnit.NOUN;

    private int hash;

    private static List<String> list;

    GramUnit(int index) {
        this.hash = index;
    }

    public int getIndex() {
        return hash;
    }

    public static int defaultIndex() {
        return DEFAULT_UNIT.getIndex();
    }

    public static List<String> toList() {
        list = new ArrayList<String>();
        GramUnit[] arr = values();
        for (int i = 0; i < arr.length; i++) {
            list.add(arr[i].name().toLowerCase());
        }
        return list;
    }

    public static int count() {
        return values().length;
    }

    public static String elementNameAt(int index) {
        if (list == null)
            toList();
        return list.get(index);
    }

    public static GramUnit elementAt(int index) {
        try {
            return values()[index];
        } catch (IndexOutOfBoundsException ex) {
            return DEFAULT_UNIT;
        }
    }

}