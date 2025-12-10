package nl.saxion.game.mazesahur.event;

/**
 * Supported horror event types that can be triggered locally or by the server.
 */
public enum HorrorEventType {
    POWER_OUTAGE,
    WHISPER,
    ENEMY_RUSH,
    HALLUCINATION_SHADOW;

    public static HorrorEventType fromString(final String raw) {
        for (HorrorEventType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }
}
