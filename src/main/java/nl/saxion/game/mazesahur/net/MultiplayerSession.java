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
}
