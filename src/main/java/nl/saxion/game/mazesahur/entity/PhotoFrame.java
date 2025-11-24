package nl.saxion.game.mazesahur.entity;

import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.world.Maze;

/**
 * Represents a photo frame mounted on a wall.
 * Contains a commemorative photo of the elevator.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class PhotoFrame {

    /**
     * Wall direction enum for frame placement.
     */
    public enum WallFace {
        NORTH(0),   // Facing south (on north wall)
        EAST(90),   // Facing west (on east wall)
        SOUTH(180), // Facing north (on south wall)
        WEST(270);  // Facing east (on west wall)

        private final float rotationDegrees;

        WallFace(final float rotationDegrees) {
            this.rotationDegrees = rotationDegrees;
        }

        public float getRotationDegrees() {
            return rotationDegrees;
        }
    }

    private final Vector3 position;
    private final Maze maze;
    private final WallFace wallFace;

    // Frame dimensions
    private static final float FRAME_WIDTH = 1.0f;
    private static final float FRAME_HEIGHT = 0.8f;
    private static final float FRAME_DEPTH = 0.05f;
    private static final float WALL_HEIGHT = 1.5f; // Height on wall (eye level)

    /**
     * Creates a new photo frame entity.
     *
     * @param maze The game maze
     * @param x X position in world coordinates
     * @param z Z position in world coordinates
     * @param wallFace Direction the frame is facing
     */
    public PhotoFrame(final Maze maze, final float x, final float z, final WallFace wallFace) {
        this.maze = maze;
        this.position = new Vector3(x, WALL_HEIGHT, z);
        this.wallFace = wallFace;
    }

    /**
     * Gets the frame's position in world coordinates.
     *
     * @return Position vector
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Gets the wall face direction of the frame.
     *
     * @return Wall face enum
     */
    public WallFace getWallFace() {
        return wallFace;
    }

    /**
     * Gets the frame width.
     *
     * @return Frame width
     */
    public float getWidth() {
        return FRAME_WIDTH;
    }

    /**
     * Gets the frame height.
     *
     * @return Frame height
     */
    public float getHeight() {
        return FRAME_HEIGHT;
    }

    /**
     * Gets the frame depth.
     *
     * @return Frame depth
     */
    public float getDepth() {
        return FRAME_DEPTH;
    }
}
