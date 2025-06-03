package get.wordy.core.api.exception;

public class DuplicateVocabularyException extends RuntimeException {
    public DuplicateVocabularyException(String name) {
        super("Vocabulary with name '" + name + "' already exists for this owner.");
    }
}
