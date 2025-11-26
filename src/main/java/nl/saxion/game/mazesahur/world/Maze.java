package nl.saxion.game.mazesahur.world;

import nl.saxion.game.mazesahur.config.GameConfig;

/**
 * Represents the game maze/world.
 * Encapsulates maze generation and wall checking logic.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class Maze {

    public static final float CELL_SIZE = GameConfig.CELL_SIZE;

    private final int width;
    private final int height;
    private final MazeGenerator generator;
    private boolean[][] walls;

    /**
     * Creates a new maze with the specified dimensions.
     *
     * @param width Maze width in cells
     * @param height Maze height in cells
     */
    public Maze(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.generator = new MazeGenerator(width, height, System.currentTimeMillis());
    }

    /**
     * Generates the maze using recursive backtracking algorithm.
     */
    public void generate() {
        generator.generate();
        this.walls = generator.getWalls();
    }

    /**
     * Checks if the specified grid position contains a wall.
     *
     * @param x Grid X coordinate
     * @param z Grid Z coordinate
     * @return True if wall exists at position
     */
    public boolean isWall(final int x, final int z) {
        if (x < 0 || x >= width || z < 0 || z >= height) {
            return true; // Out of bounds = wall
        }
        return walls[z][x];
    }

    /**
     * Gets the maze width in cells.
     *
     * @return Width in cells
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the maze height in cells.
     *
     * @return Height in cells
     */
    public int getHeight() {
        return height;
    }

    /**
     * Converts world position to grid coordinates.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Grid coordinates as [x, z]
     */
    public int[] worldToGrid(final float worldX, final float worldZ) {
        return new int[] {
            (int) Math.floor(worldX / CELL_SIZE),
            (int) Math.floor(worldZ / CELL_SIZE)
        };
    }

    /**
     * Converts grid coordinates to world position (center of cell).
     *
     * @param gridX Grid X coordinate
     * @param gridZ Grid Z coordinate
     * @return World coordinates as [x, z]
     */
    public float[] gridToWorld(final int gridX, final int gridZ) {
        return new float[] {
            gridX * CELL_SIZE + CELL_SIZE / 2f,
            gridZ * CELL_SIZE + CELL_SIZE / 2f
        };
    }

    /**
     * Creates an opening in the maze by removing walls in a rectangular area.
     * Used for integrating the elevator into the maze walls.
     *
     * @param centerX Center X grid coordinate
     * @param centerZ Center Z grid coordinate
     * @param width Width in cells
     * @param depth Depth in cells
     */
    public void createOpening(final int centerX, final int centerZ, final int width, final int depth) {
        final int halfWidth = width / 2;
        final int halfDepth = depth / 2;

        for (int z = centerZ - halfDepth; z <= centerZ + halfDepth; z++) {
            for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
                if (x >= 0 && x < this.width && z >= 0 && z < this.height) {
                    walls[z][x] = false; // Remove wall
                }
            }
        }

        System.out.println("[Maze] Created opening at grid (" + centerX + ", " + centerZ
            + ") with size " + width + "x" + depth);
    }

    /**
     * Finds and prepares a guaranteed elevator position in the maze.
     * This method searches for a suitable wall near the player spawn and creates
     * the necessary openings for the elevator.
     *
     * @param playerGridX Player spawn X in grid coordinates
     * @param playerGridZ Player spawn Z in grid coordinates
     * @return Array with elevator position info: [wallX, wallZ, openX, openZ, directionIndex]
     *         where direction: 0=North, 1=East, 2=South, 3=West
     */
    public int[] findAndPrepareElevatorPosition(final int playerGridX, final int playerGridZ) {
        // Search pattern: spiral outward from player
        final int[][] searchOffsets = {
            {0, 5}, {0, 6}, {0, 7},    // North
            {5, 0}, {6, 0}, {7, 0},    // East
            {0, -5}, {0, -6}, {0, -7}, // South
            {-5, 0}, {-6, 0}, {-7, 0}, // West
            {4, 4}, {5, 5}, {-4, 4}, {-5, 5} // Diagonals
        };

        // Wall direction vectors
        final int[][] wallDirections = {
            {0, -1},  // North - wall to north, door faces south
            {1, 0},   // East - wall to east, door faces west
            {0, 1},   // South - wall to south, door faces north
            {-1, 0}   // West - wall to west, door faces east
        };

        // Try each search position
        for (final int[] offset : searchOffsets) {
            final int openX = playerGridX + offset[0];
            final int openZ = playerGridZ + offset[1];

            // Check bounds
            if (openX < 3 || openX >= width - 3 || openZ < 3 || openZ >= height - 3) {
                continue;
            }

            // This position should be open (path)
            if (isWall(openX, openZ)) {
                continue;
            }

            // Check each direction for a wall
            for (int dirIdx = 0; dirIdx < wallDirections.length; dirIdx++) {
                final int[] wallDir = wallDirections[dirIdx];
                final int wallX = openX + wallDir[0];
                final int wallZ = openZ + wallDir[1];

                // Check if there's a wall here
                if (!isWall(wallX, wallZ)) {
                    continue; // No wall in this direction
                }

                // Found a suitable wall! Now prepare it for elevator
                // Create openings for elevator body (in wall) and door access (in open space)
                createOpening(wallX, wallZ, 2, 2);  // Clear wall for elevator
                createOpening(openX, openZ, 2, 2);  // Clear path for access

                System.out.println("[Maze] Found elevator position:");
                System.out.println("  Wall grid: (" + wallX + ", " + wallZ + ")");
                System.out.println("  Open grid: (" + openX + ", " + openZ + ")");
                System.out.println("  Direction: " + dirIdx + " (0=N,1=E,2=S,3=W)");

                return new int[] {wallX, wallZ, openX, openZ, dirIdx};
            }
        }

        // Fallback: force create a position
        System.out.println("[Maze] WARNING: No suitable wall found, forcing elevator position");
        final int fallbackX = playerGridX;
        final int fallbackZ = playerGridZ + 6;

        // Create a wall if needed
        if (!isWall(fallbackX, fallbackZ)) {
            walls[fallbackZ][fallbackX] = true;
        }

        createOpening(fallbackX, fallbackZ, 2, 2);
        createOpening(fallbackX, fallbackZ + 1, 2, 2);

        return new int[] {fallbackX, fallbackZ, fallbackX, fallbackZ + 1, 0};
    }
}

