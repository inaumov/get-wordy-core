package get.wordy.core.api.bean;

public record WordsheetHeader(
        int wordsheetId,
        String name,
        boolean isShared,
        int wordsTotal
) {
}
