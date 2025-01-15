package get.wordy.core.api.exception;

public class VocabNotFoundException extends RuntimeException {

    public VocabNotFoundException(String message) {
        super(message);
    }

    public VocabNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
