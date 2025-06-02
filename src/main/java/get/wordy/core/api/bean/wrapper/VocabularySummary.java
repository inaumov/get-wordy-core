package get.wordy.core.api.bean.wrapper;

import java.time.LocalDateTime;

public record VocabularySummary(
        String ownerId,
        int notSharedCount,
        LocalDateTime lastUpdatedAt
) {
}
