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

    // Door frame collision zones (walls beside the door)
    private static final float DOOR_WIDTH = 2.5f; // Width of the actual door opening
    private static final float WALL_THICKNESS = 0.3f; // Thickness of wall segments beside door

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

    /**
     * Checks if a position collides with the elevator's door frame walls.
     * The door frame consists of walls on both sides of the door opening.
     * This prevents players from walking through the walls beside the door.
     *
     * @param testPosition Position to test
     * @param playerRadius Collision radius of the player
     * @return True if position collides with door frame
     */
    public boolean collidesWithDoorFrame(final Vector3 testPosition, final float playerRadius) {
        // The elevator door faces +X direction (out from elevator)
        // Visual wall segments are at: elevator.x + 0.27f (the door frame front)
        // Left wall at Z: elevator.z - 6.24f
        // Right wall at Z: elevator.z + 6.24f

        // Based on rendering code:
        // - Wall segments extend from elevator center in Z direction
        // - Each wall segment is about 5m tall and positioned at specific Z offsets
        // - We need collision for the parts BESIDE the door, not in front

        final float doorFrontX = position.x + 0.27f; // Where the wall segments are rendered
        final float halfDoorWidth = DOOR_WIDTH / 2f; // 1.25m on each side of center

        // Define the wall collision zones (only on the SIDES of the door)
        // These should match the visual wall positions from MazeRenderer

        // Left wall zone (at Z = elevator.z - 6.24f, extends in X direction deeper into elevator)
        final float leftWallZ = position.z - 6.24f;
        final float leftWallWidth = 2.5f; // Width of wall segment in Z
        final float leftWallDepth = 3.0f; // Depth into elevator in X

        // Right wall zone (at Z = elevator.z + 6.24f, extends in X direction deeper into elevator)
        final float rightWallZ = position.z + 6.24f;
        final float rightWallWidth = 2.5f; // Width of wall segment in Z
        final float rightWallDepth = 3.0f; // Depth into elevator in X

        // Check collision with left wall (only blocks to the LEFT side, not in front of door)
        if (checkBoxCollision(testPosition, playerRadius,
                position.x - leftWallDepth, doorFrontX,
                leftWallZ - leftWallWidth/2f, leftWallZ + leftWallWidth/2f)) {
            return true;
        }

        // Check collision with right wall (only blocks to the RIGHT side, not in front of door)
        if (checkBoxCollision(testPosition, playerRadius,
                position.x - rightWallDepth, doorFrontX,
                rightWallZ - rightWallWidth/2f, rightWallZ + rightWallWidth/2f)) {
            return true;
        }

        return false;
    }

    /**
     * Helper method to check collision between a circle (player) and an axis-aligned box (wall).
     *
     * @param position Player position
     * @param radius Player collision radius
     * @param minX Minimum X of box
     * @param maxX Maximum X of box
     * @param minZ Minimum Z of box
     * @param maxZ Maximum Z of box
     * @return True if collision detected
     */
    private boolean checkBoxCollision(final Vector3 position, final float radius,
                                     final float minX, final float maxX,
                                     final float minZ, final float maxZ) {
        // Find closest point on box to the circle center
        final float closestX = Math.max(minX, Math.min(position.x, maxX));
        final float closestZ = Math.max(minZ, Math.min(position.z, maxZ));

        // Calculate distance from closest point to circle center
        final float dx = position.x - closestX;
        final float dz = position.z - closestZ;
        final float distanceSquared = dx * dx + dz * dz;

        return distanceSquared < radius * radius;
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
