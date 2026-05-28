package get.wordy.core.api.bean;

import java.time.Instant;

public record Theme(
        int themeId,
        String name,
        String notes, // optional richer context
        ThemeStatus status, // DRAFT, GENERATING, READY, FAILED
        Instant generatedAt,
        int wordsTotal
) {
}
