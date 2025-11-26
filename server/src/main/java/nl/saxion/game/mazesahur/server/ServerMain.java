package nl.saxion.game.mazesahur.server;

/**
 * Entry point for the multiplayer relay/authoritative server.
 * Starts a Netty WebSocket server on the given PORT (default 8080).
 */
public final class ServerMain {

    private ServerMain() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(final String[] args) throws InterruptedException {
        final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        final GameServer gameServer = new GameServer(port);
        gameServer.start();
    }
}
