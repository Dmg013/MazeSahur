package nl.saxion.game.mazesahur.event;

import java.util.UUID;

/**
 * DTO describing a horror event, either server-issued or locally generated.
 */
public class HorrorEvent {
    private final String id;
    private final HorrorEventType type;
    private final String scope; // "all" or player id (for future use)
    private final float duration;
    private final float intensity;
    private final long seed;
    private final boolean serverOrigin;

    public HorrorEvent(final String id,
                       final HorrorEventType type,
                       final String scope,
                       final float duration,
                       final float intensity,
                       final long seed,
                       final boolean serverOrigin) {
        this.id = id;
        this.type = type;
        this.scope = scope;
        this.duration = duration;
        this.intensity = intensity;
        this.seed = seed;
        this.serverOrigin = serverOrigin;
    }

    public static HorrorEvent local(final HorrorEventType type,
                                    final float duration,
                                    final float intensity) {
        return new HorrorEvent(UUID.randomUUID().toString(), type, "self", duration, intensity,
            System.currentTimeMillis(), false);
    }

    public String getId() {
        return id;
    }

    public HorrorEventType getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public float getDuration() {
        return duration;
    }

    public float getIntensity() {
        return intensity;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isServerOrigin() {
        return serverOrigin;
    }
}
