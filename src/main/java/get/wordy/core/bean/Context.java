package get.wordy.core.bean;

public record Context(
        int id,
        String example,
        int wordId
) {

    public Context withId(int id) {
        return new Context(id, example, wordId);
    }

}