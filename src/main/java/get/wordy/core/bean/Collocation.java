package get.wordy.core.bean;

public record Collocation(
        int id,
        String example,
        int wordId
) {

    public Collocation withId(int id) {
        return new Collocation(id, example, wordId);
    }
}