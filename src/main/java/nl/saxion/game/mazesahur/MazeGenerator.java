package nl.saxion.game.mazesahur;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a random maze using recursive backtracking algorithm.
 * The maze is represented as a 2D grid where true means wall and false means path.
 */
public class MazeGenerator {
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // N, E, S, W
    private final int width;
    private final int height;
    private final boolean[][] walls;
    private final Random random;

    /**
     * Creates a new maze generator.
     *
     * @param width Width of the maze (must be odd)
     * @param height Height of the maze (must be odd)
     * @param seed Random seed for maze generation
     */
    public MazeGenerator(final int width, final int height, final long seed) {
        this.width = width;
        this.height = height;
        this.walls = new boolean[height][width];
        this.random = new Random(seed);
    }

    /**
     * Generates the maze using recursive backtracking with extra connections.
     */
    public void generate() {
        // Initialize all cells as walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                walls[y][x] = true;
            }
        }

        // Start from position (1, 1)
        final int startX = 1;
        final int startY = 1;
        carvePassages(startX, startY);

        // Add many extra connections to create loops and more choices
        addExtraConnections();
    }

    /**
     * Adds extra connections to the maze to create loops and multiple paths.
     * This makes the maze more interesting with many more choices.
     */
    private void addExtraConnections() {
        // Remove only 8% of walls to add some loops without creating open spaces
        final int totalWalls = countInternalWalls();
        final int wallsToRemove = (int) (totalWalls * 0.08);

        int removed = 0;
        int attempts = 0;
        final int maxAttempts = totalWalls * 10;

        while (removed < wallsToRemove && attempts < maxAttempts) {
            attempts++;

            // Pick a random internal position
            final int x = random.nextInt(width - 2) + 1;
            final int y = random.nextInt(height - 2) + 1;

            // Only remove walls that create proper maze corridors
            if (walls[y][x] && canRemoveWall(x, y) && !createsOpenSpace(x, y)) {
                walls[y][x] = false;
                removed++;
            }
        }
    }

    /**
     * Checks if removing this wall would create an open space.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if it would create an open space
     */
    private boolean createsOpenSpace(final int x, final int y) {
        // Check if removing this wall creates a 2x2 or larger open area
        // Count open neighbors (including diagonals)
        int openCount = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                final int checkX = x + dx;
                final int checkY = y + dy;
                if (checkX > 0 && checkX < width - 1 && checkY > 0 && checkY < height - 1) {
                    if (!walls[checkY][checkX]) {
                        openCount++;
                    }
                }
            }
        }
        // If more than 4 neighbors are open, this would create too much open space
        return openCount > 4;
    }

    /**
     * Counts the number of internal walls (not on edges).
     *
     * @return Number of internal walls
     */
    private int countInternalWalls() {
        int count = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (walls[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Checks if a wall can be safely removed.
     * A wall can be removed if it connects two path areas.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if wall can be removed
     */
    private boolean canRemoveWall(final int x, final int y) {
        if (x <= 0 || x >= width - 1 || y <= 0 || y >= height - 1) {
            return false;
        }

        // Count adjacent paths
        int pathCount = 0;
        if (!walls[y - 1][x]) {
            pathCount++; // North
        }
        if (!walls[y + 1][x]) {
            pathCount++; // South
        }
        if (!walls[y][x - 1]) {
            pathCount++; // West
        }
        if (!walls[y][x + 1]) {
            pathCount++; // East
        }

        // Only remove if it connects paths (at least 2 adjacent paths)
        return pathCount >= 2;
    }

    /**
     * Recursively carves passages through the maze.
     *
     * @param x Current x position
     * @param y Current y position
     */
    private void carvePassages(final int x, final int y) {
        walls[y][x] = false; // Mark current cell as path

        // Get shuffled directions
        final List<int[]> directions = getShuffledDirections();

        for (final int[] direction : directions) {
            final int newX = x + direction[0] * 2;
            final int newY = y + direction[1] * 2;

            // Check if the new position is valid and unvisited
            if (isValid(newX, newY) && walls[newY][newX]) {
                // Carve path between current and new cell
                walls[y + direction[1]][x + direction[0]] = false;
                carvePassages(newX, newY);
            }
        }
    }

    /**
     * Gets a shuffled list of directions.
     *
     * @return Shuffled directions
     */
    private List<int[]> getShuffledDirections() {
        final List<int[]> directions = new ArrayList<>();
        for (final int[] direction : DIRECTIONS) {
            directions.add(direction);
        }

        // Fisher-Yates shuffle
        for (int i = directions.size() - 1; i > 0; i--) {
            final int j = random.nextInt(i + 1);
            final int[] temp = directions.get(i);
            directions.set(i, directions.get(j));
            directions.set(j, temp);
        }

        return directions;
    }

    /**
     * Checks if coordinates are within maze bounds.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if valid
     */
    private boolean isValid(final int x, final int y) {
        return x > 0 && x < width - 1 && y > 0 && y < height - 1;
    }

    /**
     * Checks if there's a wall at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if wall, false if path or out of bounds
     */
    public boolean isWall(final int x, final int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return true; // Treat out of bounds as walls
        }
        return walls[y][x];
    }

    /**
     * Gets the width of the maze.
     *
     * @return Maze width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the maze.
     *
     * @return Maze height
     */
    public int getHeight() {
        return height;
    }
}
