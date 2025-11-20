package nl.saxion.game.mazesahur.ai;

/**
 * Cardinal directions for rail-based movement.
 * Used to constrain enemy rotation to rollercoaster-like movement.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public enum RailDirection {
    NORTH(0, -1, 0f),
    EAST(1, 0, 90f),
    SOUTH(0, 1, 180f),
    WEST(-1, 0, 270f);

    private final int dx;
    private final int dz;
    private final float yaw;

    RailDirection(final int dx, final int dz, final float yaw) {
        this.dx = dx;
        this.dz = dz;
        this.yaw = yaw;
    }

    public int getDx() {
        return dx;
    }

    public int getDz() {
        return dz;
    }

    public float getYaw() {
        return yaw;
    }

    /**
     * Gets the opposite direction (180 degree turn).
     *
     * @return The opposite direction
     */
    public RailDirection opposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
            default: return this;
        }
    }

    /**
     * Gets the left turn direction (90 degree counterclockwise).
     *
     * @return The direction after turning left
     */
    public RailDirection turnLeft() {
        switch (this) {
            case NORTH: return WEST;
            case WEST: return SOUTH;
            case SOUTH: return EAST;
            case EAST: return NORTH;
            default: return this;
        }
    }

    /**
     * Gets the right turn direction (90 degree clockwise).
     *
     * @return The direction after turning right
     */
    public RailDirection turnRight() {
        switch (this) {
            case NORTH: return EAST;
            case EAST: return SOUTH;
            case SOUTH: return WEST;
            case WEST: return NORTH;
            default: return this;
        }
    }

    /**
     * Calculates the rotation cost to change from this direction to another.
     * Prefers straight movement, penalizes turns.
     *
     * @param to Target direction
     * @return Cost multiplier (1.0 for straight, 1.5 for 90° turn, 2.0 for 180° turn)
     */
    public double rotationCost(final RailDirection to) {
        if (this == to) {
            return 1.0; // No rotation
        } else if (this.turnLeft() == to || this.turnRight() == to) {
            return 1.5; // 90 degree turn
        } else {
            return 2.0; // 180 degree turn
        }
    }

    /**
     * Gets the direction from one position to another.
     *
     * @param dx Delta X (should be -1, 0, or 1)
     * @param dz Delta Z (should be -1, 0, or 1)
     * @return The corresponding direction, or null if invalid delta
     */
    public static RailDirection fromDelta(final int dx, final int dz) {
        if (dx == 0 && dz == -1) {
            return SOUTH;
        }
        if (dx == 1 && dz == 0) {
            return EAST;
        }
        if (dx == 0 && dz == 1) {
            return NORTH;
        }
        if (dx == -1 && dz == 0) {
            return WEST;
        }
        return null;
    }
}
