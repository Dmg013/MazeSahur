package nl.saxion.game.mazesahur.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.world.Maze;

/**
 * Represents the player entity in the maze.
 * Handles movement, collision detection, and player state.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class Player {

    private final Vector3 position;
    private final Vector3 velocity;
    private boolean boostActive;
    private float boostTimer;
    private float boostSpeedMultiplier;

    /**
     * Creates a new player at the specified position.
     *
     * @param startPosition Initial position in world space
     */
    public Player(final Vector3 startPosition) {
        this.position = new Vector3(startPosition);
        this.velocity = new Vector3();
        this.boostActive = false;
        this.boostTimer = 0f;
        this.boostSpeedMultiplier = 1.0f;
    }

    /**
     * Moves the player in the specified direction with collision detection.
     *
     * @param direction Normalized movement direction
     * @param delta Time since last frame
     */
    public void move(final Vector3 direction, final float delta) {
        // Calculate movement vector with boost multiplier
        final float effectiveSpeed = GameConfig.PLAYER_MOVE_SPEED * boostSpeedMultiplier;
        final Vector3 movement = direction.cpy().scl(effectiveSpeed * delta);
        velocity.set(movement);
    }

    /**
     * Updates player state (called every frame).
     *
     * @param delta Time since last frame
     * @param maze The maze for collision detection
     */
    public void update(final float delta, final Maze maze) {
        // Update boost timer
        if (boostActive) {
            boostTimer -= delta;
            if (boostTimer <= 0f) {
                boostActive = false;
                boostTimer = 0f;
                boostSpeedMultiplier = 1.0f;
            }
        }

        // Don't apply movement here - it's already applied in GameScreen.handleInput
        // Just clear velocity for next frame
        velocity.set(0, 0, 0);
    }

    /**
     * Checks if the player is currently moving.
     *
     * @return True if WASD keys are pressed
     */
    public boolean isMoving() {
        return Gdx.input.isKeyPressed(Input.Keys.W)
            || Gdx.input.isKeyPressed(Input.Keys.A)
            || Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.D);
    }

    /**
     * Checks collision with maze walls using circular collision detection.
     *
     * @param testPosition Position to test
     * @param maze The maze
     * @return True if collision detected
     */
    private boolean checkCollision(final Vector3 testPosition, final Maze maze) {
        final int gridX = (int) Math.floor(testPosition.x / Maze.CELL_SIZE);
        final int gridZ = (int) Math.floor(testPosition.z / Maze.CELL_SIZE);

        // Check 3x3 grid around player
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                final int checkX = gridX + dx;
                final int checkZ = gridZ + dz;

                if (checkX >= 0 && checkX < maze.getWidth()
                    && checkZ >= 0 && checkZ < maze.getHeight()
                    && maze.isWall(checkX, checkZ)) {

                    // Wall bounds
                    final float wallMinX = checkX * Maze.CELL_SIZE;
                    final float wallMaxX = wallMinX + Maze.CELL_SIZE;
                    final float wallMinZ = checkZ * Maze.CELL_SIZE;
                    final float wallMaxZ = wallMinZ + Maze.CELL_SIZE;

                    // Closest point on wall to player
                    final float closestX = Math.max(wallMinX, Math.min(testPosition.x, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(testPosition.z, wallMaxZ));

                    // Distance check
                    final float dx2 = testPosition.x - closestX;
                    final float dz2 = testPosition.z - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;

                    if (distSquared < GameConfig.PLAYER_COLLISION_RADIUS * GameConfig.PLAYER_COLLISION_RADIUS) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gets the player's current position.
     *
     * @return Position vector (reference, do not modify)
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Gets the player's height above ground.
     *
     * @return Height value
     */
    public static float getHeight() {
        return GameConfig.PLAYER_HEIGHT;
    }

    /**
     * Activates a speed boost for the player.
     *
     * @param duration Boost duration in seconds
     * @param multiplier Speed multiplier
     */
    public void activateBoost(final float duration, final float multiplier) {
        this.boostActive = true;
        this.boostTimer = duration;
        this.boostSpeedMultiplier = multiplier;
    }

    /**
     * Checks if the player currently has an active boost.
     *
     * @return true if boost is active
     */
    public boolean isBoostActive() {
        return boostActive;
    }

    /**
     * Gets the remaining boost time.
     *
     * @return Remaining boost time in seconds
     */
    public float getBoostTimeRemaining() {
        return boostTimer;
    }

    /**
     * Gets the current speed multiplier.
     *
     * @return Speed multiplier
     */
    public float getSpeedMultiplier() {
        return boostSpeedMultiplier;
    }

    /**
     * Gets the player's collision radius.
     *
     * @return Collision radius
     */
    public static float getCollisionRadius() {
        return GameConfig.PLAYER_COLLISION_RADIUS;
    }
}

