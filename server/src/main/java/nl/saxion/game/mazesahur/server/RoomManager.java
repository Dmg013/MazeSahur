package nl.saxion.game.mazesahur.server;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.saxion.game.mazesahur.server.protocol.Messages.InputMessage;
import nl.saxion.game.mazesahur.server.protocol.Messages.JoinRequest;

/**
 * Extremely small placeholder for room management.
 * Now includes a fixed tick loop and basic join/input/state handling.
 */
public class RoomManager {

    private static final float TICK_RATE = 30f;
    private static final float DELTA = 1f / TICK_RATE;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Channel, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public RoomManager() {
        executor.scheduleAtFixedRate(this::tickAll, 0, (long) (1000 / TICK_RATE), TimeUnit.MILLISECONDS);
    }

    public void handleConnect(final Channel channel) {
        System.out.println("[RoomManager] New connection: " + channel.remoteAddress());
    }

    public void handleDisconnect(final Channel channel) {
        final PlayerSession session = sessions.remove(channel);
        if (session != null) {
            session.getRoom().removePlayer(session.getPlayerId(), channel);
            System.out.println("[RoomManager] Disconnect: " + channel.remoteAddress()
                + " (room " + session.getRoom().getRoomId() + ")");
        } else {
            System.out.println("[RoomManager] Disconnect: " + channel.remoteAddress());
        }
    }

    public void handleMessage(final Channel channel, final String payload) {
        try {
            final JsonNode root = mapper.readTree(payload);
            final String type = Optional.ofNullable(root.get("type"))
                .map(JsonNode::asText)
                .orElse("");

            switch (type) {
                case "join":
                    handleJoin(channel, root);
                    break;
                case "input":
                    handleInput(channel, root);
                    break;
                default:
                    System.out.println("[RoomManager] Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[RoomManager] Failed to parse message: " + e.getMessage());
        }
    }

    public void handleError(final Channel channel, final Throwable error) {
        System.err.println("[RoomManager] Error from " + channel.remoteAddress() + ": " + error.getMessage());
    }

    private void handleJoin(final Channel channel, final JsonNode root) {
        final JoinRequest req = mapper.convertValue(root, JoinRequest.class);
        if (req.room == null || req.name == null) {
            channel.writeAndFlush(new TextWebSocketFrame("{\"type\":\"error\",\"reason\":\"missing room/name\"}"));
            return;
        }

        final Room room = rooms.computeIfAbsent(req.room, id -> new Room(id, deriveSeed(id), mapper));
        final PlayerSession session = room.addPlayer(channel, req.name);
        sessions.put(channel, session);

        final var joined = room.buildJoinedResponse();
        joined.playerId = session.getPlayerId();
        final String json;
        try {
            json = mapper.writeValueAsString(joined);
        } catch (Exception e) {
            channel.writeAndFlush(new TextWebSocketFrame("{\"type\":\"error\",\"reason\":\"join serialize failed\"}"));
            return;
        }

        channel.writeAndFlush(new TextWebSocketFrame(json));
        System.out.println("[RoomManager] " + req.name + " joined room " + room.getRoomId()
            + " as " + session.getPlayerId() + " seed=" + room.getSeed());
    }

    private void handleInput(final Channel channel, final JsonNode root) {
        final PlayerSession session = sessions.get(channel);
        if (session == null) {
            channel.writeAndFlush(new TextWebSocketFrame("{\"type\":\"error\",\"reason\":\"not joined\"}"));
            return;
        }
        final InputMessage input = mapper.convertValue(root, InputMessage.class);
        session.setLastSeq(input.seq);
        session.getRoom().updateInput(session.getPlayerId(), input);
    }

    private long deriveSeed(final String roomId) {
        // Simple deterministic seed from room id
        return Objects.hash(roomId);
    }

    private void tickAll() {
        for (Room room : rooms.values()) {
            room.tick(DELTA);
        }
    }
}
