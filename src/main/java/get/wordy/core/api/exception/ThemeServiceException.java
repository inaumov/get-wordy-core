package get.wordy.core.api.exception;

public class ThemeServiceException extends RuntimeException {
    public ThemeServiceException() {
    }

    public ThemeServiceException(String errorMessage) {
        super(errorMessage);
    }
}
