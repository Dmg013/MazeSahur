package nl.saxion.game.mazesahur.net;

/**
 * Simple DTO for player transforms received from the server.
 */
public class RemotePlayerState {
    public String id;
    public String name;
    public float x;
    public float y;
    public float z;
    public float yaw;
}
