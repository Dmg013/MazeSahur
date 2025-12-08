package nl.saxion.game.mazesahur.net;

/**
 * Callback interface for network events.
 * Actual protocol handling (state updates, join acks, etc.) can be implemented by the client.
 */
public interface NetworkClientCallback {
    void onConnected();
    void onMessage(String message);
    void onDisconnected(int statusCode, String reason);
    void onError(Throwable error);
}
