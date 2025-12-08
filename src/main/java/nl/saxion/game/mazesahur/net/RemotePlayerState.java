package nl.saxion.game.mazesahur.net;

/**
 * Simple DTO for player transforms and animation state received from the server.
 */
public class RemotePlayerState {
    public String id;
    public String name;
    public float x;
    public float y;
    public float z;
    public float yaw;
    public String animation; // Current animation name ("idle", "walking", "running", etc.)
    public boolean isMoving; // Whether the player is currently moving
    public boolean isRunning; // Whether the player is running/sprinting
    public String characterType; // Character skin/model type (e.g., "DEFAULT", "BIG_BUSINESS")
}
