package get.wordy.core.api.exception;

public class DuplicateThemeException extends RuntimeException {
    public DuplicateThemeException(String name) {
        super("Theme with name '" + name + "' already exists for this owner.");
    }
}
