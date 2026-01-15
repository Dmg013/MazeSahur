package nl.saxion.game.mazesahur.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import nl.saxion.game.mazesahur.model.CharacterType;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.server.protocol.Messages.InputMessage;
import nl.saxion.game.mazesahur.server.protocol.Messages.JoinedResponse;
import nl.saxion.game.mazesahur.server.protocol.Messages.LevelChangeMessage;
import nl.saxion.game.mazesahur.server.protocol.Messages.PlayerState;
import nl.saxion.game.mazesahur.server.protocol.Messages.StateMessage;
import nl.saxion.game.mazesahur.server.protocol.Messages.EnemyState;
import nl.saxion.game.mazesahur.world.Maze;

/**
 * Represents a multiplayer room and contains the authoritative state.
 * This is intentionally lightweight: only player transforms are simulated server-side.
 */
public class Room {

    private final String roomId;
    private long seed;
    private Maze maze;
    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private final Map<String, InputMessage> latestInputs = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Channel> channels = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper;
    private final Player proxyPlayer;
    private final Enemy enemy;

    // Level system
    private int currentLevel = 1;
    private float exitX;
    private float exitZ;
    private float exitCooldown = 0f;
    private static final float EXIT_TRIGGER_RADIUS = 2.5f;
    private static final float EXIT_COOLDOWN_DURATION = 3.0f;
    private static final int MAX_LEVELS = 1;

    public Room(final String roomId, final long seed, final ObjectMapper mapper) {
        this.roomId = roomId;
        this.seed = seed;
        this.mapper = mapper;
        this.maze = new Maze(GameConfig.MAZE_SIZE, GameConfig.MAZE_SIZE, seed);
        this.maze.generate();
        
        // Find exit location for level 1
        findAndSetExitLocation();
        
        this.proxyPlayer = new Player(new Vector3(12f, GameConfig.PLAYER_HEIGHT, 12f));
        this.enemy = new Enemy(maze, proxyPlayer);
        this.enemy.initialize();
    }

    public String getRoomId() {
        return roomId;
    }

    public long getSeed() {
        return seed;
    }

    public Maze getMaze() {
        return maze;
    }

    public PlayerSession addPlayer(final Channel channel, final String playerName, final String characterType) {
        final String playerId = UUID.randomUUID().toString();
        final PlayerState state = new PlayerState();
        state.id = playerId;
        state.name = playerName;
        state.characterType = characterType != null && !characterType.isEmpty()
            ? characterType
            : CharacterType.DEFAULT.name();
        final float[] spawn = defaultSpawnPosition();
        state.x = spawn[0];
        state.y = GameConfig.PLAYER_HEIGHT;
        state.z = spawn[1];
        state.yaw = 0f;
        players.put(playerId, state);
        channels.add(channel);
        return new PlayerSession(channel, playerId, this);
    }

    public void removePlayer(final String playerId, final Channel channel) {
        players.remove(playerId);
        latestInputs.remove(playerId);
        channels.remove(channel);
    }

    public void updateInput(final String playerId, final InputMessage input) {
        latestInputs.put(playerId, input);
    }

    /**
     * Runs one simulation step and broadcasts the resulting state.
     *
     * @param deltaSeconds fixed timestep
     */
    public void tick(final float deltaSeconds) {
        // Update exit cooldown timer
        if (exitCooldown > 0f) {
            exitCooldown -= deltaSeconds;
        }

        // Apply inputs to positions
        for (Map.Entry<String, PlayerState> entry : players.entrySet()) {
            final String playerId = entry.getKey();
            final PlayerState state = entry.getValue();
            final InputMessage input = latestInputs.get(playerId);
            if (input != null) {
                applyMovement(state, input, deltaSeconds);
            }
        }

        // Check if any player reached the exit (only if cooldown expired)
        if (exitCooldown <= 0f && checkAnyPlayerAtExit()) {
            handleLevelTransition();
            exitCooldown = EXIT_COOLDOWN_DURATION;
            // After level transition, new state will be broadcast automatically
        }

        // Update proxy target to nearest player
        final PlayerState target = nearestPlayerToEnemy();
        if (target != null) {
            proxyPlayer.getPosition().set(target.x, target.y, target.z);
        }

        // Update enemy AI
        enemy.update(deltaSeconds);

        // Build snapshot
        final StateMessage stateMsg = new StateMessage();
        stateMsg.type = "state";
        stateMsg.ts = System.currentTimeMillis();
        stateMsg.players = new ArrayList<>(players.values());
        stateMsg.enemy = buildEnemyState();
        stateMsg.currentLevel = currentLevel;

        // Broadcast
        final String json;
        try {
            json = mapper.writeValueAsString(stateMsg);
        } catch (Exception e) {
            System.err.println("[Room " + roomId + "] Failed to serialize state: " + e.getMessage());
            return;
        }

        final TextWebSocketFrame frame = new TextWebSocketFrame(json);
        for (Channel ch : channels) {
            if (ch.isActive()) {
                ch.writeAndFlush(frame.retainedDuplicate());
            }
        }
        frame.release();
    }

    public JoinedResponse buildJoinedResponse() {
        final JoinedResponse joined = new JoinedResponse();
        joined.type = "joined";
        joined.room = roomId;
        joined.seed = seed;
        joined.players = new ArrayList<>(players.values());
        joined.currentLevel = currentLevel;
        joined.exitX = exitX;
        joined.exitZ = exitZ;
        return joined;
    }

    private EnemyState buildEnemyState() {
        final EnemyState es = new EnemyState();
        es.x = enemy.getPosition().x;
        es.y = enemy.getPosition().y;
        es.z = enemy.getPosition().z;
        es.yaw = enemy.getYaw();
        return es;
    }

    private void applyMovement(final PlayerState state, final InputMessage input, final float deltaSeconds) {
        // Normalize input vector
        float dx = input.moveX;
        float dz = input.moveZ;
        final float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-3f) {
            dx /= len;
            dz /= len;
        }

        final float speed = GameConfig.PLAYER_MOVE_SPEED;
        final float moveStepX = dx * speed * deltaSeconds;
        final float moveStepZ = dz * speed * deltaSeconds;

        // Try full move
        if (!collides(state.x + moveStepX, state.z + moveStepZ)) {
            state.x += moveStepX;
            state.z += moveStepZ;
        } else if (!collides(state.x + moveStepX, state.z)) {
            state.x += moveStepX;
        } else if (!collides(state.x, state.z + moveStepZ)) {
            state.z += moveStepZ;
        }

        state.yaw = input.yaw;
    }

    private boolean collides(final float nextX, final float nextZ) {
        final int gridX = (int) Math.floor(nextX / Maze.CELL_SIZE);
        final int gridZ = (int) Math.floor(nextZ / Maze.CELL_SIZE);

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                final int checkX = gridX + dx;
                final int checkZ = gridZ + dz;
                if (checkX >= 0 && checkX < maze.getWidth()
                    && checkZ >= 0 && checkZ < maze.getHeight()
                    && maze.isWall(checkX, checkZ)) {
                    final float wallMinX = checkX * Maze.CELL_SIZE;
                    final float wallMaxX = wallMinX + Maze.CELL_SIZE;
                    final float wallMinZ = checkZ * Maze.CELL_SIZE;
                    final float wallMaxZ = wallMinZ + Maze.CELL_SIZE;
                    final float closestX = Math.max(wallMinX, Math.min(nextX, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(nextZ, wallMaxZ));
                    final float dx2 = nextX - closestX;
                    final float dz2 = nextZ - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;
                    if (distSquared < GameConfig.PLAYER_COLLISION_RADIUS * GameConfig.PLAYER_COLLISION_RADIUS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private float[] defaultSpawnPosition() {
        // Spawn near the same coordinates as singleplayer (12,12)
        return new float[] {12f, 12f};
    }

    /**
     * Zoekt een geschikte exit locatie: ver van spawn (12, 12), in een open vakje.
     * Strategie: Zoek de verste open tegel van de spawn.
     */
    private void findAndSetExitLocation() {
        // Spawn is at world position (12, 12), NOT grid position
        final float spawnX = 12f;
        final float spawnZ = 12f;

        float bestDist = 0f;
        int bestGridX = -1;
        int bestGridZ = -1;

        // Itereer door alle cellen en vind de verste open tegel
        for (int z = 1; z < maze.getHeight() - 1; z++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (!maze.isWall(x, z)) {
                    // Converteer naar world coords
                    final float worldX = x * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;
                    final float worldZ = z * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;

                    final float dx = worldX - spawnX;
                    final float dz = worldZ - spawnZ;
                    final float dist = (float) Math.sqrt(dx * dx + dz * dz);

                    if (dist > bestDist) {
                        bestDist = dist;
                        bestGridX = x;
                        bestGridZ = z;
                    }
                }
            }
        }

        // Fallback als geen exit gevonden (zou niet moeten gebeuren)
        if (bestGridX == -1) {
            bestGridX = maze.getWidth() - 2;
            bestGridZ = maze.getHeight() - 2;
        }

        exitX = bestGridX * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;
        exitZ = bestGridZ * Maze.CELL_SIZE + Maze.CELL_SIZE / 2f;

        System.out.println("[Room] Level " + currentLevel + " exit at: (" + exitX + ", " + exitZ + ")");
    }

    /**
     * Controleert of een van de spelers de exit heeft bereikt.
     */
    private boolean checkAnyPlayerAtExit() {
        for (PlayerState state : players.values()) {
            final float dx = state.x - exitX;
            final float dz = state.z - exitZ;
            final float distSquared = dx * dx + dz * dz;

            if (distSquared < EXIT_TRIGGER_RADIUS * EXIT_TRIGGER_RADIUS) {
                System.out.println("[Room] Player " + state.name + " reached exit!");
                return true;
            }
        }
        return false;
    }

    /**
     * Voert een level transitie uit: verhoog level, nieuwe maze, reset posities.
     */
    private void handleLevelTransition() {
        currentLevel++;

        // Als max levels bereikt, loop terug naar 1
        if (currentLevel > MAX_LEVELS) {
            System.out.println("[Room] Max level reached! Looping back to level 1.");
            currentLevel = 1;
        }

        System.out.println("[Room] === LEVEL TRANSITION TO LEVEL " + currentLevel + " ===");

        // Genereer nieuwe seed (deterministic maar verschillend per level)
        final long baseSeed = System.currentTimeMillis();
        seed = baseSeed ^ currentLevel;

        // Regenereer maze
        maze = new Maze(GameConfig.MAZE_SIZE, GameConfig.MAZE_SIZE, seed);
        maze.generate();

        // Vind nieuwe exit
        findAndSetExitLocation();

        // Reset alle spelerposities naar spawn
        final float spawnX = 12f;
        final float spawnZ = 12f;
        for (PlayerState state : players.values()) {
            state.x = spawnX;
            state.y = GameConfig.PLAYER_HEIGHT;
            state.z = spawnZ;
            state.yaw = 0f;
        }

        // Reset enemy
        proxyPlayer.getPosition().set(spawnX, GameConfig.PLAYER_HEIGHT, spawnZ);
        enemy.initialize();

        // Broadcast level change event
        broadcastLevelChange();
    }

    /**
     * Stuurt een LevelChangeMessage naar alle clients.
     */
    private void broadcastLevelChange() {
        final LevelChangeMessage msg = new LevelChangeMessage();
        msg.type = "level_change";
        msg.newLevel = currentLevel;
        msg.newSeed = seed;
        msg.exitX = exitX;
        msg.exitZ = exitZ;
        msg.spawnX = 12f;
        msg.spawnZ = 12f;

        final String json;
        try {
            json = mapper.writeValueAsString(msg);
        } catch (Exception e) {
            System.err.println("[Room] Failed to serialize level_change: " + e.getMessage());
            return;
        }

        final TextWebSocketFrame frame = new TextWebSocketFrame(json);
        for (Channel ch : channels) {
            if (ch.isActive()) {
                ch.writeAndFlush(frame.retainedDuplicate());
            }
        }
        frame.release();

        System.out.println("[Room] Broadcasted level_change to " + channels.size() + " clients");
    }

    private PlayerState nearestPlayerToEnemy() {
        PlayerState nearest = null;
        float best = Float.MAX_VALUE;
        for (PlayerState ps : players.values()) {
            final float dx = ps.x - enemy.getPosition().x;
            final float dz = ps.z - enemy.getPosition().z;
            final float dist2 = dx * dx + dz * dz;
            if (dist2 < best) {
                best = dist2;
                nearest = ps;
            }
        }
        return nearest;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Room room = (Room) o;
        return Objects.equals(roomId, room.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }
}
