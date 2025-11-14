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
}

