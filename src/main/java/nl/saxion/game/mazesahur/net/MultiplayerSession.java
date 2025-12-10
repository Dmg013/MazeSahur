package nl.saxion.game.mazesahur.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import nl.saxion.game.mazesahur.event.HorrorEvent;
import nl.saxion.game.mazesahur.event.HorrorEventType;
import nl.saxion.game.mazesahur.model.CharacterType;

/**
 * Manages a multiplayer WebSocket session: join, input sending, and receiving state snapshots.
 */
public class MultiplayerSession implements NetworkClientCallback {

    private final NetworkSessionConfig config;
    private final NetworkClient networkClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong seq = new AtomicLong(1);
    private final Map<String, RemotePlayerState> players = new ConcurrentHashMap<>();
    private final CountDownLatch joinLatch = new CountDownLatch(1);
    private volatile String playerId;
    private volatile long seed;
    private volatile boolean joined = false;
    private volatile EnemySnapshot enemySnapshot;
    private final ConcurrentLinkedQueue<HorrorEvent> incomingEvents = new ConcurrentLinkedQueue<>();

    public MultiplayerSession(final NetworkSessionConfig config) {
        this.config = config;
        this.networkClient = new NetworkClient(URI.create(config.getServerUrl()), config.getRoomId(), config.getPlayerName(), this);
    }

    /**
     * Connects and waits for the joined message (blocking up to timeout).
     */
    public boolean connectAndAwaitJoin(final Duration timeout) {
        try {
            networkClient.connect().join();
            return joinLatch.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isJoined() {
        return joined;
    }

    public String getPlayerId() {
        return playerId;
    }

    public long getSeed() {
        return seed;
    }

    public void sendInput(final float moveX, final float moveZ, final float yaw) {
        if (!joined) return;
        final InputMessage msg = new InputMessage();
        msg.type = "input";
        msg.seq = seq.getAndIncrement();
        msg.moveX = moveX;
        msg.moveZ = moveZ;
        msg.yaw = yaw;
        try {
            networkClient.sendText(mapper.writeValueAsString(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a snapshot of remote players (excluding self).
     */
    public List<RemotePlayerState> getRemotePlayers() {
        final List<RemotePlayerState> list = new ArrayList<>();
        for (RemotePlayerState state : players.values()) {
            if (!state.id.equals(playerId)) {
                list.add(state);
            }
        }
        return list;
    }

    /**
     * Gets the latest authoritative state for this player (may be null before first state).
     */
    public RemotePlayerState getSelfState() {
        return players.get(playerId);
    }

    public EnemySnapshot getEnemySnapshot() {
        return enemySnapshot;
    }

    /**
     * Sends lightweight context to the server so it can pace events server-side.
     */
    public void sendContext(final float stressLevel, final boolean flashlightOn, final float enemyDistance) {
        if (!joined) {
            return;
        }
        final ContextMessage msg = new ContextMessage();
        msg.type = "context";
        msg.stress = stressLevel;
        msg.flashlight = flashlightOn;
        msg.enemyDistance = enemyDistance;
        try {
            networkClient.sendText(mapper.writeValueAsString(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and clears pending events received from the server.
     */
    public List<HorrorEvent> drainEvents() {
        final List<HorrorEvent> list = new ArrayList<>();
        HorrorEvent event;
        while ((event = incomingEvents.poll()) != null) {
            list.add(event);
        }
        return list;
    }

    @Override
    public void onConnected() {
        // Send join immediately
        final JoinMessage joinMsg = new JoinMessage();
        joinMsg.type = "join";
        joinMsg.room = config.getRoomId();
        joinMsg.name = config.getPlayerName();
        final CharacterType characterType = config.getCharacterType();
        joinMsg.characterType = characterType != null ? characterType.name() : CharacterType.DEFAULT.name();
        try {
            networkClient.sendText(mapper.writeValueAsString(joinMsg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(final String message) {
        try {
            final JsonNode root = mapper.readTree(message);
            final String type = Optional.ofNullable(root.get("type")).map(JsonNode::asText).orElse("");
            switch (type) {
                case "joined":
                    handleJoined(root);
                    break;
                case "state":
                    handleState(root);
                    break;
                case "event":
                    handleEvent(root);
                    break;
                default:
                    System.out.println("[MultiplayerSession] Unknown message type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(final int statusCode, final String reason) {
        joined = false;
        System.out.println("[MultiplayerSession] Disconnected: " + statusCode + " " + reason);
    }

    @Override
    public void onError(final Throwable error) {
        error.printStackTrace();
    }

    private void handleJoined(final JsonNode root) {
        playerId = root.path("playerId").asText();
        seed = root.path("seed").asLong();
        final JsonNode playersNode = root.path("players");
        if (playersNode.isArray()) {
            for (JsonNode node : playersNode) {
                final RemotePlayerState state = mapper.convertValue(node, RemotePlayerState.class);
                players.put(state.id, state);
            }
        }
        joined = true;
        joinLatch.countDown();
        System.out.println("[MultiplayerSession] Joined room " + root.path("room").asText()
            + " as " + playerId + " seed=" + seed);
    }

    private void handleState(final JsonNode root) {
        final JsonNode playersNode = root.path("players");
        if (playersNode.isArray()) {
            for (JsonNode node : playersNode) {
                final RemotePlayerState state = mapper.convertValue(node, RemotePlayerState.class);
                players.put(state.id, state);
            }
        }
        final JsonNode enemyNode = root.path("enemy");
        if (enemyNode != null && !enemyNode.isMissingNode()) {
            enemySnapshot = mapper.convertValue(enemyNode, EnemySnapshot.class);
        }
    }

    private void handleEvent(final JsonNode root) {
        final HorrorEventType type = HorrorEventType.fromString(root.path("eventType").asText());
        if (type == null) {
            System.out.println("[MultiplayerSession] Unknown event type payload: " + root);
            return;
        }
        final String id = Optional.ofNullable(root.get("id")).map(JsonNode::asText)
            .orElse("evt-" + System.currentTimeMillis());
        final String scope = Optional.ofNullable(root.get("scope")).map(JsonNode::asText).orElse("all");
        final float duration = (float) root.path("duration").asDouble(3.0);
        final float intensity = (float) root.path("intensity").asDouble(1.0);
        final long eventSeed = root.path("seed").asLong(System.currentTimeMillis());

        incomingEvents.add(new HorrorEvent(id, type, scope, duration, intensity, eventSeed, true));
    }

    // Message DTOs for serialization
    private static final class JoinMessage {
        public String type;
        public String room;
        public String name;
        public String characterType;
    }

    private static final class InputMessage {
        public String type;
        public long seq;
        public float moveX;
        public float moveZ;
        public float yaw;
    }

    private static final class ContextMessage {
        public String type;
        public float stress;
        public boolean flashlight;
        public float enemyDistance;
    }
}
