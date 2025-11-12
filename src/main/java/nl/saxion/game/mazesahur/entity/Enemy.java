package nl.saxion.game.mazesahur.entity;

import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.ai.PathfindingService;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.world.Maze;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents the enemy entity that chases the player.
 * Features intelligent AI with multiple behavior states.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class Enemy {

    /**
     * AI behavior states for the enemy.
     */
    public enum AIState {
        WANDERING,   // Moving randomly
        CHASING,     // Direct line of sight to player
        PURSUING,    // Going to last known position
        PATHFINDING  // Using A* to navigate maze
    }

    private final Vector3 position;
    private final Maze maze;
    private final Player player;
    private final Random random;

    // AI state
    private AIState currentState;
    private final Vector3 lastKnownPlayerPosition;
    private final Vector3 wanderTarget;
    private float timeSincePlayerSeen;
    private float pathUpdateTimer;
    private float wanderUpdateTimer;

    // Pathfinding
    private List<int[]> currentPath;
    private int pathIndex;

    // Movement
    private float yaw;

    // Stuck detection
    private final Vector3 lastPosition;
    private float stuckTimer;
    private static final float STUCK_THRESHOLD = 0.5f;

    /**
     * Creates a new enemy entity.
     *
     * @param maze The game maze
     * @param player Reference to the player
     */
    public Enemy(final Maze maze, final Player player) {
        this.maze = maze;
        this.player = player;
        this.position = new Vector3();
        this.random = new Random();
        this.currentState = AIState.WANDERING;
        this.lastKnownPlayerPosition = new Vector3();
        this.wanderTarget = new Vector3();
        this.lastPosition = new Vector3();
        this.currentPath = new ArrayList<>();
        this.timeSincePlayerSeen = GameConfig.ENEMY_CHASE_MEMORY_DURATION + 1;
        this.pathUpdateTimer = 0;
        this.wanderUpdateTimer = 0;
        this.pathIndex = 0;
        this.stuckTimer = 0;
    }

    /**
     * Initializes the enemy at a valid spawn position far from the player.
     */
    public void initialize() {
        findValidSpawnPosition();
        lastPosition.set(position);
        updateWanderTarget();
    }

    /**
     * Updates enemy AI and movement.
     *
     * @param delta Time since last frame
     */
    public void update(final float delta) {
        updateAI(delta);
        updateMovement(delta);
        updateSahurTransform();
    }

    /**
     * Updates AI behavior based on player visibility and state.
     */
    private void updateAI(final float delta) {
        // Check line of sight to player
        final boolean canSeePlayer = PathfindingService.hasLineOfSight(
            maze,
            position.x, position.z,
            player.getPosition().x, player.getPosition().z,
            Maze.CELL_SIZE
        );

        if (canSeePlayer) {
            timeSincePlayerSeen = 0;
            lastKnownPlayerPosition.set(player.getPosition());
            currentState = AIState.CHASING;
        } else {
            timeSincePlayerSeen += delta;

            if (currentState == AIState.CHASING) {
                currentState = AIState.PURSUING;
            }

            if (timeSincePlayerSeen > GameConfig.ENEMY_CHASE_MEMORY_DURATION
                && currentState == AIState.PURSUING) {
                currentState = AIState.WANDERING;
                wanderUpdateTimer = 0;
            }
        }

        // Update behavior timers
        pathUpdateTimer += delta;
        wanderUpdateTimer += delta;
    }

    /**
     * Updates enemy movement based on current AI state.
     */
    private void updateMovement(final float delta) {
        Vector3 targetDirection = null;

        switch (currentState) {
            case CHASING:
                targetDirection = new Vector3(
                    player.getPosition().x - position.x,
                    0,
                    player.getPosition().z - position.z
                );
                break;

            case PURSUING:
                targetDirection = new Vector3(
                    lastKnownPlayerPosition.x - position.x,
                    0,
                    lastKnownPlayerPosition.z - position.z
                );

                if (targetDirection.len() < 2f) {
                    currentState = AIState.PATHFINDING;
                    updatePathToPlayer();
                }
                break;

            case WANDERING:
                if (wanderUpdateTimer >= GameConfig.WANDER_UPDATE_INTERVAL) {
                    wanderUpdateTimer = 0;
                    updateWanderTarget();
                }

                if (wanderTarget != null) {
                    targetDirection = new Vector3(
                        wanderTarget.x - position.x,
                        0,
                        wanderTarget.z - position.z
                    );

                    if (targetDirection.len() < 1f) {
                        updateWanderTarget();
                    }
                }

                // Occasionally use pathfinding
                if (pathUpdateTimer >= GameConfig.PATH_UPDATE_INTERVAL) {
                    pathUpdateTimer = 0;
                    currentState = AIState.PATHFINDING;
                    updatePathToPlayer();
                }
                break;

            case PATHFINDING:
                if (currentPath != null && !currentPath.isEmpty() && pathIndex < currentPath.size()) {
                    final int[] targetGrid = currentPath.get(pathIndex);
                    final float targetX = targetGrid[0] * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;
                    final float targetZ = targetGrid[1] * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;

                    targetDirection = new Vector3(targetX - position.x, 0, targetZ - position.z);

                    if (targetDirection.len() < 1f) {
                        pathIndex++;
                        if (pathIndex >= currentPath.size()) {
                            if (timeSincePlayerSeen < GameConfig.ENEMY_CHASE_MEMORY_DURATION) {
                                currentState = AIState.PURSUING;
                            } else {
                                currentState = AIState.WANDERING;
                            }
                        }
                    }
                } else {
                    if (timeSincePlayerSeen < GameConfig.ENEMY_CHASE_MEMORY_DURATION) {
                        currentState = AIState.PURSUING;
                    } else {
                        currentState = AIState.WANDERING;
                    }
                }
                break;
        }

        // Stuck detection
        stuckTimer += delta;
        if (stuckTimer > STUCK_THRESHOLD) {
            final float distanceMoved = position.dst(lastPosition);
            if (distanceMoved < 0.5f) {
                handleStuck();
            }
            lastPosition.set(position);
            stuckTimer = 0;
        }

        // Apply movement
        if (targetDirection != null && targetDirection.len() > 0.1f) {
            yaw = (float) Math.toDegrees(Math.atan2(targetDirection.x, targetDirection.z));

            targetDirection.nor().scl(GameConfig.ENEMY_SPEED * delta);
            final Vector3 newPosition = position.cpy().add(targetDirection);

            if (!checkCollision(newPosition)) {
                position.set(newPosition);
            } else {
                // Try sliding along walls
                final Vector3 slideX = position.cpy().add(targetDirection.x, 0, 0);
                if (!checkCollision(slideX)) {
                    position.set(slideX);
                } else {
                    final Vector3 slideZ = position.cpy().add(0, 0, targetDirection.z);
                    if (!checkCollision(slideZ)) {
                        position.set(slideZ);
                    }
                }
            }
        }

        // Safety check
        if (checkCollision(position)) {
            position.set(lastPosition);
            System.out.println("Warning: Enemy clipped into wall, reverting to last position");
        }
    }

    /**
     * Updates enemy transform (called after movement).
     */
    private void updateSahurTransform() {
        // This is just for internal tracking, actual rendering transform is in MazeRenderer
    }

    /**
     * Finds a valid spawn position far from the player.
     */
    private void findValidSpawnPosition() {
        final int[] playerGrid = maze.worldToGrid(player.getPosition().x, player.getPosition().z);

        int bestX = -1;
        int bestZ = -1;
        float maxDistance = 0;

        for (int z = 0; z < maze.getHeight(); z++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.isWall(x, z)) {
                    continue;
                }

                // Check valid neighbors
                int validNeighbors = 0;
                final int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
                for (final int[] dir : directions) {
                    final int nx = x + dir[0];
                    final int nz = z + dir[1];
                    if (nx >= 0 && nx < maze.getWidth() && nz >= 0 && nz < maze.getHeight()
                        && !maze.isWall(nx, nz)) {
                        validNeighbors++;
                    }
                }

                if (validNeighbors < 2) {
                    continue;
                }

                final float dx = x - playerGrid[0];
                final float dz = z - playerGrid[1];
                final float distance = (float) Math.sqrt(dx * dx + dz * dz);

                if (distance > maxDistance) {
                    maxDistance = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
        }

        if (bestX >= 0 && bestZ >= 0) {
            final float[] worldPos = maze.gridToWorld(bestX, bestZ);
            position.set(worldPos[0], GameConfig.ENEMY_HEIGHT, worldPos[1]);
            System.out.println("Enemy spawned at grid (" + bestX + ", " + bestZ + ") - "
                + (int)maxDistance + " cells from player");
        } else {
            final float[] centerPos = maze.gridToWorld(maze.getWidth() / 2, maze.getHeight() / 2);
            position.set(centerPos[0], GameConfig.ENEMY_HEIGHT, centerPos[1]);
            System.out.println("Enemy spawned at maze center (fallback)");
        }
    }

    /**
     * Updates the wander target to a random valid adjacent cell.
     */
    private void updateWanderTarget() {
        final int[] currentGrid = maze.worldToGrid(position.x, position.z);

        if (currentGrid[0] < 0 || currentGrid[0] >= maze.getWidth()
            || currentGrid[1] < 0 || currentGrid[1] >= maze.getHeight()) {
            final float[] centerPos = maze.gridToWorld(maze.getWidth() / 2, maze.getHeight() / 2);
            wanderTarget.set(centerPos[0], GameConfig.ENEMY_HEIGHT, centerPos[1]);
            return;
        }

        final int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0},
                                     {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        final List<int[]> validDirections = new ArrayList<>();

        for (final int[] dir : directions) {
            final int targetX = currentGrid[0] + dir[0];
            final int targetZ = currentGrid[1] + dir[1];

            if (targetX >= 0 && targetX < maze.getWidth()
                && targetZ >= 0 && targetZ < maze.getHeight()
                && !maze.isWall(targetX, targetZ)) {
                validDirections.add(new int[]{targetX, targetZ});
            }
        }

        if (!validDirections.isEmpty()) {
            final int[] chosen = validDirections.get(random.nextInt(validDirections.size()));
            final float[] worldPos = maze.gridToWorld(chosen[0], chosen[1]);
            wanderTarget.set(worldPos[0], GameConfig.ENEMY_HEIGHT, worldPos[1]);
        } else {
            wanderTarget.set(position);
            System.out.println("Warning: Enemy has no valid wander targets at ("
                + currentGrid[0] + ", " + currentGrid[1] + ")");
        }
    }

    /**
     * Updates the A* path to the player's position.
     */
    private void updatePathToPlayer() {
        final int[] enemyGrid = maze.worldToGrid(position.x, position.z);
        final int[] playerGrid = maze.worldToGrid(player.getPosition().x, player.getPosition().z);

        if (enemyGrid[0] != playerGrid[0] || enemyGrid[1] != playerGrid[1]) {
            currentPath = PathfindingService.findPath(maze, enemyGrid[0], enemyGrid[1],
                                                      playerGrid[0], playerGrid[1]);
            pathIndex = 0;
        } else {
            currentState = AIState.WANDERING;
        }
    }

    /**
     * Handles when enemy gets stuck.
     */
    private void handleStuck() {
        if (currentState == AIState.WANDERING || currentState == AIState.PURSUING) {
            updateWanderTarget();
        } else if (currentState == AIState.PATHFINDING) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                currentState = AIState.WANDERING;
                updateWanderTarget();
            }
        }
    }

    /**
     * Checks collision with maze walls using larger radius for enemy.
     */
    private boolean checkCollision(final Vector3 testPosition) {
        final int[] grid = maze.worldToGrid(testPosition.x, testPosition.z);

        // Check wider area due to larger collision radius
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                final int checkX = grid[0] + dx;
                final int checkZ = grid[1] + dz;

                if (maze.isWall(checkX, checkZ)) {
                    final float[] wallWorld = maze.gridToWorld(checkX, checkZ);
                    final float wallMinX = wallWorld[0] - Maze.CELL_SIZE / 2f;
                    final float wallMaxX = wallWorld[0] + Maze.CELL_SIZE / 2f;
                    final float wallMinZ = wallWorld[1] - Maze.CELL_SIZE / 2f;
                    final float wallMaxZ = wallWorld[1] + Maze.CELL_SIZE / 2f;

                    final float closestX = Math.max(wallMinX, Math.min(testPosition.x, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(testPosition.z, wallMaxZ));

                    final float dx2 = testPosition.x - closestX;
                    final float dz2 = testPosition.z - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;

                    if (distSquared < GameConfig.ENEMY_COLLISION_RADIUS * GameConfig.ENEMY_COLLISION_RADIUS) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Getters
    public Vector3 getPosition() {
        return position;
    }

    public AIState getCurrentState() {
        return currentState;
    }

    public float getYaw() {
        return yaw;
    }

    public float getTimeSincePlayerSeen() {
        return timeSincePlayerSeen;
    }

    public float getCatchRadius() {
        return GameConfig.ENEMY_CATCH_RADIUS;
    }
}

