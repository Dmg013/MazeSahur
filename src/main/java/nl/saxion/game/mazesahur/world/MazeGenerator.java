package nl.saxion.game.mazesahur.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a random maze using recursive backtracking algorithm.
 * The maze is represented as a 2D grid where true means wall and false means path.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
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
     * @param width  Width of the maze in cells
     * @param height Height of the maze in cells
     * @param seed   Random seed for reproducible mazes
     */
    public MazeGenerator(final int width, final int height, final long seed) {
        this.width = width;
        this.height = height;
        this.walls = new boolean[height][width];
        this.random = new Random(seed);
    }

    /**
     * Generates the maze using recursive backtracking algorithm.
     * After generation, the maze is stored in the walls array.
     */
    public void generate() {
        // Initialize all cells as walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                walls[y][x] = true;
            }
        }

        // Start from (1, 1)
        carve(1, 1);
    }

    /**
     * Recursively carves paths through the maze using backtracking.
     *
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    private void carve(final int x, final int y) {
        walls[y][x] = false;

        // Get neighbors in random order
        final List<int[]> directions = new ArrayList<>();
        for (final int[] dir : DIRECTIONS) {
            directions.add(dir);
        }
        // Shuffle directions for randomness
        for (int i = directions.size() - 1; i > 0; i--) {
            final int j = random.nextInt(i + 1);
            final int[] temp = directions.get(i);
            directions.set(i, directions.get(j));
            directions.set(j, temp);
        }

        // Try each direction
        for (final int[] dir : directions) {
            final int nx = x + dir[0] * 2; // Move 2 cells to ensure walls between
            final int ny = y + dir[1] * 2;

            // Check bounds
            if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1 && walls[ny][nx]) {
                // Carve wall between current and next cell
                walls[y + dir[1]][x + dir[0]] = false;
                // Recursively carve from next cell
                carve(nx, ny);
            }
        }
    }

    /**
     * Gets the generated maze walls.
     *
     * @return 2D array where true = wall, false = path
     */
    public boolean[][] getWalls() {
        return walls;
    }

    /**
     * Gets the maze width.
     *
     * @return Width in cells
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the maze height.
     *
     * @return Height in cells
     */
    public int getHeight() {
        return height;
    }
}

