package nl.saxion.game.mazesahur.model;

/**
 * Enum representing available character skins/models for players.
 * Each character has its own model files and display name.
 *
 * @author Tim
 * @version 1.0
 */
public enum CharacterType {
    DEFAULT("Default", "models/player/default"),
    BIG_BUSINESS("Big Business", "models/player/big_business"),
    SOUNDCLOUD("Soundcloud", "models/player/soundcloud"),
    LOCKDOWN("Lockdown", "models/player/lockdown"),
    MAXIMILIAN("Maximilian", "models/player/maximilian");

    private final String displayName;
    private final String modelPath;

    CharacterType(final String displayName, final String modelPath) {
        this.displayName = displayName;
        this.modelPath = modelPath;
    }

    /**
     * Gets the display name for this character (shown in UI).
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the model path for this character.
     *
     * @return Path to model directory
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Gets the rarity tier for this character.
     *
     * @return Rarity tier
     */
    public Rarity getRarity() {
        switch (this) {
            case DEFAULT:
                return Rarity.COMMON;
            case BIG_BUSINESS:
                return Rarity.RARE;
            case SOUNDCLOUD:
                return Rarity.RARE;
            case LOCKDOWN:
                return Rarity.EPIC;
            case MAXIMILIAN:
                return Rarity.LEGENDARY;
            default:
                return Rarity.COMMON;
        }
    }

    /**
     * Gets character type from string name (case-insensitive).
     * Returns DEFAULT if not found.
     *
     * @param name Name to parse
     * @return Matching character type or DEFAULT
     */
    public static CharacterType fromString(final String name) {
        if (name == null) {
            return DEFAULT;
        }

        for (CharacterType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return DEFAULT;
    }
}
