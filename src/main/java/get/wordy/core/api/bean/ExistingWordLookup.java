package get.wordy.core.api.bean;

public record ExistingWordLookup(
        WordKey key,
        Integer id,
        String level
) {
    public boolean exists() {
        return id != null;
    }
}