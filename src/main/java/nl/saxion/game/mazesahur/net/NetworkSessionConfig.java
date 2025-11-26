package nl.saxion.game.mazesahur.net;

/**
 * Immutable config object for joining a multiplayer room.
 */
public class NetworkSessionConfig {
    private final String serverUrl;
    private final String roomId;
    private final String playerName;

    public NetworkSessionConfig(final String serverUrl, final String roomId, final String playerName) {
        this.serverUrl = serverUrl;
        this.roomId = roomId;
        this.playerName = playerName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlayerName() {
        return playerName;
    }
}
