package nl.saxion.game.mazesahur.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Minimal WebSocket client scaffold for multiplayer.
 * Handles message fragmentation by buffering partial messages until complete.
 * The actual protocol (join/input/state) is layered on top of this class.
 */
public class NetworkClient implements Listener {

    private final URI serverUri;
    private final String roomId;
    private final String playerName;
    private final NetworkClientCallback callback;
    private final HttpClient httpClient;
    private WebSocket webSocket;
    private final StringBuilder messageBuffer = new StringBuilder();

    public NetworkClient(final URI serverUri,
                         final String roomId,
                         final String playerName,
                         final NetworkClientCallback callback) {
        this.serverUri = Objects.requireNonNull(serverUri, "serverUri");
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.callback = Objects.requireNonNull(callback, "callback");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Opens a WebSocket connection to the server.
     *
     * @return future that completes when the socket is ready
     */
    public CompletableFuture<Void> connect() {
        return httpClient.newWebSocketBuilder()
            .buildAsync(serverUri, this)
            .thenAccept(ws -> {
                webSocket = ws;
                callback.onConnected();
            });
    }

    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed();
    }

    /**
     * Sends a pre-serialized message to the server.
     * Caller is responsible for JSON encoding.
     */
    public void sendText(final String message) {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket not connected");
        }
        webSocket.sendText(message, true);
    }

    /**
     * Closes the connection gracefully.
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "client exit");
        }
    }

    @Override
    public void onOpen(final WebSocket webSocket) {
        // Request more messages
        webSocket.request(1);
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(final WebSocket webSocket,
                                     final CharSequence data,
                                     final boolean last) {
        // Append fragment to buffer
        messageBuffer.append(data);

        // Only process complete messages (when last == true)
        if (last) {
            try {
                callback.onMessage(messageBuffer.toString());
            } catch (Exception e) {
                System.err.println("[NetworkClient] Error processing message: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Clear buffer for next message
                messageBuffer.setLength(0);
            }
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(final WebSocket webSocket,
                                       final ByteBuffer data,
                                       final boolean last) {
        // Not used yet, but keep the request to avoid stalling
        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(final WebSocket webSocket, final Throwable error) {
        callback.onError(error);
    }

    @Override
    public CompletionStage<?> onClose(final WebSocket webSocket,
                                      final int statusCode,
                                      final String reason) {
        callback.onDisconnected(statusCode, reason);
        return Listener.super.onClose(webSocket, statusCode, reason);
    }
}
