package get.wordy.core.api.exception;

public class ClassInfoNotFoundException extends RuntimeException {

    public ClassInfoNotFoundException(String message) {
        super(message);
    }

    public ClassInfoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
