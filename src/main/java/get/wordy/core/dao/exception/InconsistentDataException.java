package get.wordy.core.dao.exception;

public class InconsistentDataException extends RuntimeException {

    /**
     * Constructs a {@code InconsistentDataException} with no message
     */
    public InconsistentDataException() {
        super();
    }

    /**
     * Constructs a {@code InconsistentDataException} with the specified message
     *
     * @param s message
     */
    public InconsistentDataException(String s) {
        super(s);
    }

}
