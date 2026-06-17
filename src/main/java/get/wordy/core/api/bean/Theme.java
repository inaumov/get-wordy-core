package get.wordy.core.api.bean;

import java.time.Instant;

public record Theme(
        int themeId,
        String name,
        String notes, // optional richer context
        ThemeStatus status,
        Instant generatedAt,
        Instant lastUpdatedAt,
        int wordsTotal
) {
}
