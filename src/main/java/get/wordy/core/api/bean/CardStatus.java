package get.wordy.core.api.bean;

public enum CardStatus {

    EDIT(0),
    POSTPONED(1),
    TO_LEARN(2),
    LEARNT(3);

    public static final CardStatus DEFAULT_STATUS = CardStatus.EDIT;

    private int index;

    CardStatus(int index) {
        this.index = index;
    }

    public static String getValue(CardStatus status) {
        String replace = status.name().replace("_", " ");
        replace = replace.substring(0, 1) + replace.substring(1).toLowerCase();
        return replace;
    }

    public int getIndex() {
        return index;
    }

    public static int defaultIndex() {
        return DEFAULT_STATUS.getIndex();
    }

    public static String elementNameAt(int index) {
        return values()[index].name();
    }

    public static CardStatus elementAt(int index) {
        try {
            return values()[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return DEFAULT_STATUS;
        }
    }

}