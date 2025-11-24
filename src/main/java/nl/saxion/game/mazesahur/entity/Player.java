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
    private boolean isSprinting;

    /**
     * Creates a new player at the specified position.
     *
     * @param startPosition Initial position in world space
     */
    public Player(final Vector3 startPosition) {
        this.position = new Vector3(startPosition);
        this.velocity = new Vector3();
        this.isSprinting = false;
    }

    /**
     * Moves the player in the specified direction with collision detection.
     *
     * @param direction Normalized movement direction
     * @param delta Time since last frame
     */
    public void move(final Vector3 direction, final float delta) {
        // Calculate movement vector
        final Vector3 movement = direction.cpy().scl(GameConfig.PLAYER_MOVE_SPEED * delta);
        velocity.set(movement);
    }

    /**
     * Updates player state (called every frame).
     *
     * @param delta Time since last frame
     * @param maze The maze for collision detection
     */
    public void update(final float delta, final Maze maze) {
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
     * Gets the player's collision radius.
     *
     * @return Collision radius
     */
    public static float getCollisionRadius() {
        return GameConfig.PLAYER_COLLISION_RADIUS;
    }

    /**
     * Sets whether the player is sprinting.
     *
     * @param sprinting True if sprinting, false otherwise
     */
    public void setSprinting(final boolean sprinting) {
        this.isSprinting = sprinting;
    }

    /**
     * Checks if the player is currently sprinting.
     *
     * @return True if sprinting
     */
    public boolean isSprinting() {
        return isSprinting;
    }
}

