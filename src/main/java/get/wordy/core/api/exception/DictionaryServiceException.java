package get.wordy.core.api.exception;

public class DictionaryServiceException extends RuntimeException {
    public DictionaryServiceException() {
    }

    public DictionaryServiceException(String errorMessage) {
        super(errorMessage);
    }
}
