package get.wordy.core.api.id;

import java.util.Set;

public record OwnersId(
        Set<String> ownerIds, // multiple IDs of the owner (user or class)
        String ownerType // "user" or "class"
) {
}
