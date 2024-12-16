package get.wordy.core.api.exception;

public class WordsheetNotFoundException extends RuntimeException {

    public WordsheetNotFoundException(String message) {
        super(message);
    }

    public WordsheetNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
