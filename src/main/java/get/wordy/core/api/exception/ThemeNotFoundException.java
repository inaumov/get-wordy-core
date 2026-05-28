package get.wordy.core.api.exception;

public class ThemeNotFoundException extends RuntimeException {

    public ThemeNotFoundException(String message) {
        super(message);
    }

    public ThemeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
