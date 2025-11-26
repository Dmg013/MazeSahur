package nl.saxion.game.mazesahur.server;

import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extremely small placeholder for room management.
 * Right now it only tracks channels and logs messages; the gameplay loop/state
 * sync still needs to be implemented.
 */
public class RoomManager {

    private final Map<Channel, String> channelToRoom = new ConcurrentHashMap<>();

    public void handleConnect(final Channel channel) {
        System.out.println("[RoomManager] New connection: " + channel.remoteAddress());
    }

    public void handleDisconnect(final Channel channel) {
        final String roomId = channelToRoom.remove(channel);
        System.out.println("[RoomManager] Disconnect: " + channel.remoteAddress()
            + (roomId != null ? " (room " + roomId + ")" : ""));
    }

    public void handleMessage(final Channel channel, final String payload) {
        // TODO: parse JSON (join/input/ping) and route to rooms. For now, just log.
        System.out.println("[RoomManager] Received: " + payload);
    }

    public void handleError(final Channel channel, final Throwable error) {
        System.err.println("[RoomManager] Error from " + channel.remoteAddress() + ": " + error.getMessage());
    }
}
