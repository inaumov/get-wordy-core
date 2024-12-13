package get.wordy.core.api.id;

public record OwnerId(
        String ownerId, // ID of the owner (user or class)
        String ownerType // "user" or "class"
) {
}
