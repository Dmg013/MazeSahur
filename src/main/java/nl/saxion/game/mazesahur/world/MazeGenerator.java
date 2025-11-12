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

        // Add extra paths to create more branches and side paths
        addExtraPaths();
    }

    /**
     * Removes random walls to create additional paths, loops, and branches.
     * This makes the maze more complex with multiple routes and side paths.
     */
    private void addExtraPaths() {
        // Phase 1: Add loops and connections (remove ~12% of walls)
        final int loopCount = (width * height) / 8;

        for (int i = 0; i < loopCount; i++) {
            // Pick a random wall position (avoid edges)
            final int x = random.nextInt(width - 2) + 1;
            final int y = random.nextInt(height - 2) + 1;

            // Only remove walls that are between two paths (creates connections)
            if (walls[y][x]) {
                // Check if this wall connects two path cells (horizontal or vertical)
                boolean connectsHorizontal = !walls[y][x - 1] && !walls[y][x + 1];
                boolean connectsVertical = !walls[y - 1][x] && !walls[y + 1][x];

                // Remove wall if it connects paths (creates a loop/branch)
                if (connectsHorizontal || connectsVertical) {
                    walls[y][x] = false;
                }
            }
        }

        // Phase 2: Add dead-end branches and side paths
        final int branchCount = (width * height) / 20;

        for (int i = 0; i < branchCount; i++) {
            // Pick a random position next to an existing path
            final int x = random.nextInt(width - 2) + 1;
            final int y = random.nextInt(height - 2) + 1;

            if (walls[y][x]) {
                // Count neighboring paths
                int pathNeighbors = 0;
                if (!walls[y][x - 1]) pathNeighbors++;
                if (!walls[y][x + 1]) pathNeighbors++;
                if (!walls[y - 1][x]) pathNeighbors++;
                if (!walls[y + 1][x]) pathNeighbors++;

                // Create side branch if this wall has exactly 1 path neighbor
                // This creates interesting dead-ends and side paths
                if (pathNeighbors == 1) {
                    walls[y][x] = false;

                    // 50% chance to extend the branch one more cell
                    if (random.nextBoolean()) {
                        // Find direction away from existing paths
                        for (final int[] dir : DIRECTIONS) {
                            final int nx = x + dir[0];
                            final int ny = y + dir[1];

                            if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1 && walls[ny][nx]) {
                                // Count neighbors of potential extension
                                int extPathNeighbors = 0;
                                if (!walls[ny][nx - 1]) extPathNeighbors++;
                                if (!walls[ny][nx + 1]) extPathNeighbors++;
                                if (!walls[ny - 1][nx]) extPathNeighbors++;
                                if (!walls[ny + 1][nx]) extPathNeighbors++;

                                // Only extend if it doesn't create too many connections
                                if (extPathNeighbors <= 1) {
                                    walls[ny][nx] = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
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

