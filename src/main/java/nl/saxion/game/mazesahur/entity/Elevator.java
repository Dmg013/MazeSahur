package nl.saxion.game.mazesahur.entity;

import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.world.Maze;

/**
 * Represents an elevator entity with automatic doors.
 * Features proximity detection and collision support.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class Elevator {

    /**
     * Elevator state for door animations.
     */
    public enum ElevatorState {
        CLOSED,  // Doors are fully closed
        OPENING, // Doors are opening
        OPEN,    // Doors are fully open
        CLOSING  // Doors are closing
    }

    private final Vector3 position;
    private final Maze maze;
    private ElevatorState currentState;
    private float animationTimer;
    private float doorOpenTimer;

    // Animation timing
    private static final float DOOR_ANIMATION_DURATION = 2.0f; // 2 seconds to open/close
    private static final float DOOR_OPEN_HOLD_TIME = 3.0f; // Hold open for 3 seconds
    private static final float PROXIMITY_DISTANCE = 5.0f; // Distance to trigger door opening

    // Elevator dimensions
    private static final float ELEVATOR_WIDTH = 3.0f;
    private static final float ELEVATOR_DEPTH = 3.0f;
    private static final float ELEVATOR_HEIGHT = 4.0f;

    /**
     * Creates a new elevator entity.
     *
     * @param maze The game maze
     * @param x X position in world coordinates
     * @param z Z position in world coordinates
     */
    public Elevator(final Maze maze, final float x, final float z) {
        this.maze = maze;
        this.position = new Vector3(x, 0, z);
        this.currentState = ElevatorState.CLOSED;
        this.animationTimer = 0;
        this.doorOpenTimer = 0;
    }

    /**
     * Updates elevator state based on player proximity.
     *
     * @param delta Time since last frame
     * @param playerPosition Current player position
     */
    public void update(final float delta, final Vector3 playerPosition) {
        // Check distance to player
        final float distance = getDistanceToPlayer(playerPosition);

        // State machine for door behavior
        switch (currentState) {
            case CLOSED:
                // Open doors if player is nearby
                if (distance <= PROXIMITY_DISTANCE) {
                    currentState = ElevatorState.OPENING;
                    animationTimer = 0;
                }
                break;

            case OPENING:
                animationTimer += delta;
                if (animationTimer >= DOOR_ANIMATION_DURATION) {
                    currentState = ElevatorState.OPEN;
                    doorOpenTimer = 0;
                    animationTimer = DOOR_ANIMATION_DURATION; // Clamp to max
                }
                break;

            case OPEN:
                doorOpenTimer += delta;
                // Close doors if player is far away and timer expired
                if (distance > PROXIMITY_DISTANCE && doorOpenTimer >= DOOR_OPEN_HOLD_TIME) {
                    currentState = ElevatorState.CLOSING;
                    animationTimer = DOOR_ANIMATION_DURATION; // Start from fully open
                }
                // Reset timer if player comes back
                if (distance <= PROXIMITY_DISTANCE) {
                    doorOpenTimer = 0;
                }
                break;

            case CLOSING:
                animationTimer -= delta;
                if (animationTimer <= 0) {
                    currentState = ElevatorState.CLOSED;
                    animationTimer = 0;
                }
                // Re-open if player approaches while closing
                if (distance <= PROXIMITY_DISTANCE) {
                    currentState = ElevatorState.OPENING;
                }
                break;
        }
    }

    /**
     * Calculates horizontal distance to player (ignoring Y axis).
     *
     * @param playerPosition Player position
     * @return Distance in world units
     */
    public float getDistanceToPlayer(final Vector3 playerPosition) {
        final float dx = position.x - playerPosition.x;
        final float dz = position.z - playerPosition.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Gets the current door open percentage (0.0 = closed, 1.0 = fully open).
     *
     * @return Door open percentage
     */
    public float getDoorOpenPercentage() {
        return Math.max(0.0f, Math.min(1.0f, animationTimer / DOOR_ANIMATION_DURATION));
    }

    /**
     * Checks if a position is inside the elevator.
     * Used for collision detection when doors are open.
     *
     * @param testPosition Position to test
     * @return True if position is inside elevator bounds
     */
    public boolean isInsideElevator(final Vector3 testPosition) {
        final float halfWidth = ELEVATOR_WIDTH / 2f;
        final float halfDepth = ELEVATOR_DEPTH / 2f;

        return testPosition.x >= position.x - halfWidth
            && testPosition.x <= position.x + halfWidth
            && testPosition.z >= position.z - halfDepth
            && testPosition.z <= position.z + halfDepth;
    }

    /**
     * Checks if the elevator blocks movement at a given position.
     * Doors must be closed or closing to block movement.
     *
     * @param testPosition Position to test
     * @return True if elevator blocks this position
     */
    public boolean blocksPosition(final Vector3 testPosition) {
        // Only block if doors are closed or mostly closed
        if (currentState == ElevatorState.OPEN || getDoorOpenPercentage() > 0.5f) {
            return false;
        }

        return isInsideElevator(testPosition);
    }

    /**
     * Toggles the elevator doors between open and closed states.
     * Used for manual player control.
     */
    public void toggleDoors() {
        switch (currentState) {
            case CLOSED:
                // Start opening
                currentState = ElevatorState.OPENING;
                animationTimer = 0;
                System.out.println("[Elevator] Manual open triggered");
                break;

            case OPENING:
                // Reverse to closing
                currentState = ElevatorState.CLOSING;
                // Keep current animation progress but reverse direction
                System.out.println("[Elevator] Manual close triggered (during opening)");
                break;

            case OPEN:
                // Start closing
                currentState = ElevatorState.CLOSING;
                animationTimer = DOOR_ANIMATION_DURATION;
                System.out.println("[Elevator] Manual close triggered");
                break;

            case CLOSING:
                // Reverse to opening
                currentState = ElevatorState.OPENING;
                // Keep current animation progress but reverse direction
                System.out.println("[Elevator] Manual open triggered (during closing)");
                break;
        }
    }

    // Getters
    public Vector3 getPosition() {
        return position;
    }

    public ElevatorState getCurrentState() {
        return currentState;
    }

    public float getAnimationTimer() {
        return animationTimer;
    }

    public static float getElevatorWidth() {
        return ELEVATOR_WIDTH;
    }

    public static float getElevatorDepth() {
        return ELEVATOR_DEPTH;
    }

    public static float getElevatorHeight() {
        return ELEVATOR_HEIGHT;
    }
}
