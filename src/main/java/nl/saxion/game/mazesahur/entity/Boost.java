package nl.saxion.game.mazesahur.entity;

import com.badlogic.gdx.math.Vector3;

/**
 * Represents a speed boost pickup that spawns in the maze.
 * When collected by the player, grants temporary increased movement speed.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class Boost {

    /**
     * Boost state enum.
     */
    public enum BoostState {
        ACTIVE,     // Available for pickup
        COLLECTED,  // Has been collected by player
        RESPAWNING  // Waiting to respawn
    }

    private final Vector3 position;
    private BoostState state;
    private float respawnTimer;
    private float rotationAngle; // For visual spinning effect

    // Boost parameters
    private static final float PICKUP_RADIUS = 1.0f; // Distance to collect
    private static final float RESPAWN_TIME = 30.0f; // 30 seconds to respawn
    private static final float BOOST_DURATION = 8.0f; // 8 seconds of speed boost
    private static final float SPEED_MULTIPLIER = 1.8f; // 80% faster movement
    private static final float HEIGHT = 0.5f; // Height above ground

    /**
     * Creates a new boost pickup.
     *
     * @param x X position in world coordinates
     * @param z Z position in world coordinates
     */
    public Boost(final float x, final float z) {
        this.position = new Vector3(x, HEIGHT, z);
        this.state = BoostState.ACTIVE;
        this.respawnTimer = 0f;
        this.rotationAngle = 0f;
    }

    /**
     * Updates the boost state and animations.
     *
     * @param delta Time since last frame
     */
    public void update(final float delta) {
        // Rotate for visual effect
        rotationAngle += delta * 120f; // 120 degrees per second
        if (rotationAngle >= 360f) {
            rotationAngle -= 360f;
        }

        // Handle respawn timer
        if (state == BoostState.RESPAWNING) {
            respawnTimer -= delta;
            if (respawnTimer <= 0f) {
                state = BoostState.ACTIVE;
                respawnTimer = 0f;
            }
        }
    }

    /**
     * Attempts to collect the boost if player is close enough.
     *
     * @param playerPosition Player's position
     * @return true if boost was collected, false otherwise
     */
    public boolean tryCollect(final Vector3 playerPosition) {
        if (state != BoostState.ACTIVE) {
            return false;
        }

        // Check distance (ignore Y axis)
        final float dx = position.x - playerPosition.x;
        final float dz = position.z - playerPosition.z;
        final float distanceSq = dx * dx + dz * dz;

        if (distanceSq <= PICKUP_RADIUS * PICKUP_RADIUS) {
            collect();
            return true;
        }

        return false;
    }

    /**
     * Marks the boost as collected and starts respawn timer.
     */
    private void collect() {
        state = BoostState.COLLECTED;
        respawnTimer = RESPAWN_TIME;
        state = BoostState.RESPAWNING;
    }

    /**
     * Gets the boost's position in world coordinates.
     *
     * @return Position vector
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Gets the current state of the boost.
     *
     * @return Boost state
     */
    public BoostState getState() {
        return state;
    }

    /**
     * Gets the rotation angle for rendering.
     *
     * @return Rotation angle in degrees
     */
    public float getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Checks if the boost is currently active and collectible.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return state == BoostState.ACTIVE;
    }

    /**
     * Gets the respawn progress (0.0 to 1.0).
     *
     * @return Respawn progress
     */
    public float getRespawnProgress() {
        if (state != BoostState.RESPAWNING) {
            return 1.0f;
        }
        return 1.0f - (respawnTimer / RESPAWN_TIME);
    }

    /**
     * Gets the boost duration in seconds.
     *
     * @return Boost duration
     */
    public static float getBoostDuration() {
        return BOOST_DURATION;
    }

    /**
     * Gets the speed multiplier granted by this boost.
     *
     * @return Speed multiplier
     */
    public static float getSpeedMultiplier() {
        return SPEED_MULTIPLIER;
    }

    /**
     * Gets the pickup radius.
     *
     * @return Pickup radius
     */
    public static float getPickupRadius() {
        return PICKUP_RADIUS;
    }
}
