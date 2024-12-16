package get.wordy.core.api.exception;

public class ClassServiceException extends RuntimeException {

    public ClassServiceException() {
    }

    public ClassServiceException(String message) {
        super(message);
    }

    public ClassServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
