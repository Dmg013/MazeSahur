package nl.saxion.game.mazesahur.net;

/**
 * Default network settings for quick testing; override with environment variables.
 */
public final class NetworkDefaults {
    private NetworkDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String serverUrl() {
        return System.getenv().getOrDefault("MAZE_SERVER_URL", "ws://57.129.114.17:25557/ws");
    }

    public static String room() {
        return System.getenv().getOrDefault("MAZE_ROOM", "test-room");
    }

    public static String playerName() {
        return System.getenv().getOrDefault("MAZE_PLAYER_NAME", "Player" + (int) (Math.random() * 1000));
    }
}
