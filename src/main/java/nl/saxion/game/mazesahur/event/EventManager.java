package nl.saxion.game.mazesahur.event;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.world.Maze;

/**
 * Coordinates horror events and applies their effects to the game systems.
 */
public class EventManager {

    private static final float LOCAL_EVENT_COOLDOWN = 6.0f;
    private static final float LOCAL_EVENT_BASE_CHANCE = 0.25f;

    private final Maze maze;
    private final Player player;
    private final Enemy enemy;
    private final LightingManager lightingManager;
    private final boolean networked;
    private final Random random;

    private final Map<String, ActiveEvent> activeEvents = new HashMap<>();
    private final List<HorrorEvent> queuedEvents = new ArrayList<>();

    private float localEventTimer = 0f;
    private final Vector3 cameraOffset = new Vector3();

    // Optional sounds (null-safe)
    private final Sound whisperSound;
    private final Sound rushSound;
    private final Sound hallucinationSound;

    public EventManager(final Maze maze,
                        final Player player,
                        final Enemy enemy,
                        final LightingManager lightingManager,
                        final boolean networked,
                        final Sound whisperSound,
                        final Sound rushSound,
                        final Sound hallucinationSound) {
        this.maze = maze;
        this.player = player;
        this.enemy = enemy;
        this.lightingManager = lightingManager;
        this.networked = networked;
        this.random = new Random();
        this.whisperSound = whisperSound;
        this.rushSound = rushSound;
        this.hallucinationSound = hallucinationSound;
    }

    /**
    * Adds a server-driven event into the queue.
    */
    public void enqueue(final HorrorEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        queuedEvents.add(event);
    }

    /**
     * Triggers a local event (singleplayer or client-side hallucination).
     */
    public void triggerLocal(final HorrorEventType type, final float duration, final float intensity) {
        enqueue(HorrorEvent.local(type, duration, intensity));
    }

    /**
     * Main update loop. Applies queued events, updates timers, and applies effects.
     */
    public void update(final float delta, final float stressLevel) {
        cameraOffset.set(0, 0, 0);

        // Local pacing: only in singleplayer
        if (!networked) {
            maybeTriggerLocalEvent(delta, stressLevel);
        }

        // Pull queued events into active list
        for (HorrorEvent event : queuedEvents) {
            activeEvents.put(event.getId(), new ActiveEvent(event));
            startEvent(event);
        }
        queuedEvents.clear();

        // Update active events
        final Iterator<Map.Entry<String, ActiveEvent>> iterator = activeEvents.entrySet().iterator();
        boolean hasPowerOutage = false;
        while (iterator.hasNext()) {
            final Map.Entry<String, ActiveEvent> entry = iterator.next();
            final ActiveEvent active = entry.getValue();
            active.elapsed += delta;

            applyPerFrameEffect(active.getEvent(), stressLevel);

            if (active.getEvent().getType() == HorrorEventType.POWER_OUTAGE) {
                hasPowerOutage = true;
            }

            if (active.elapsed >= active.getEvent().getDuration()) {
                endEvent(active.getEvent());
                iterator.remove();
            }
        }

        // Restore lighting when no outage events remain
        if (!hasPowerOutage) {
            lightingManager.setFlashlightSuppressed(false);
            lightingManager.setIntensityMultiplier(1f);
        } else {
            lightingManager.setIntensityMultiplier(0.3f);
        }
    }

    /**
     * Gets the camera offset produced by currently running events.
     */
    public Vector3 getCameraOffset() {
        return cameraOffset;
    }

    private void maybeTriggerLocalEvent(final float delta, final float stressLevel) {
        localEventTimer -= delta;
        if (localEventTimer > 0) {
            return;
        }

        final float chance = LOCAL_EVENT_BASE_CHANCE + stressLevel * 0.4f;
        if (random.nextFloat() < chance) {
            // Choose an event weighted by stress
            final HorrorEventType type = pickLocalEventType(stressLevel);
            final float duration = 3.0f + random.nextFloat() * 2.5f;
            final float intensity = 0.6f + random.nextFloat() * 0.8f;
            triggerLocal(type, duration, intensity);
        }

        localEventTimer = LOCAL_EVENT_COOLDOWN + random.nextFloat() * 3.0f;
    }

    private HorrorEventType pickLocalEventType(final float stressLevel) {
        if (stressLevel > 0.7f) {
            return random.nextBoolean() ? HorrorEventType.ENEMY_RUSH : HorrorEventType.POWER_OUTAGE;
        }
        final float roll = random.nextFloat();
        if (roll < 0.4f) {
            return HorrorEventType.WHISPER;
        }
        if (roll < 0.7f) {
            return HorrorEventType.HALLUCINATION_SHADOW;
        }
        return HorrorEventType.POWER_OUTAGE;
    }

    private void startEvent(final HorrorEvent event) {
        switch (event.getType()) {
            case POWER_OUTAGE:
                lightingManager.setFlashlightSuppressed(true);
                lightingManager.setIntensityMultiplier(0.3f);
                break;
            case WHISPER:
                playOnce(whisperSound, 0.9f);
                break;
            case ENEMY_RUSH:
                playOnce(rushSound, 0.8f);
                teleportEnemyCloser(event.getIntensity());
                break;
            case HALLUCINATION_SHADOW:
                playOnce(hallucinationSound, 0.5f);
                break;
            default:
                break;
        }
    }

    private void applyPerFrameEffect(final HorrorEvent event, final float stressLevel) {
        switch (event.getType()) {
            case HALLUCINATION_SHADOW:
                final float shake = 0.08f * event.getIntensity();
                cameraOffset.add(
                    (random.nextFloat() - 0.5f) * shake,
                    (random.nextFloat() - 0.5f) * shake * 0.2f,
                    (random.nextFloat() - 0.5f) * shake
                );
                break;
            case WHISPER:
                // No per-frame effect, but keep subtle camera wiggle when stress is high
                if (stressLevel > 0.6f) {
                    final float micro = 0.01f * event.getIntensity();
                    cameraOffset.add(
                        (random.nextFloat() - 0.5f) * micro,
                        0,
                        (random.nextFloat() - 0.5f) * micro
                    );
                }
                break;
            default:
                break;
        }
    }

    private void endEvent(final HorrorEvent event) {
        if (event.getType() == HorrorEventType.ENEMY_RUSH) {
            // Enemy will naturally continue chasing; no teardown needed
            return;
        }
        if (event.getType() == HorrorEventType.HALLUCINATION_SHADOW) {
            cameraOffset.set(0, 0, 0);
        }
    }

    private void teleportEnemyCloser(final float intensity) {
        final Vector3 playerPos = player.getPosition();
        final int targetCells = 4 + random.nextInt(3); // 4-6 cells away
        final float desiredDistance = targetCells * Maze.CELL_SIZE;

        for (int attempt = 0; attempt < 12; attempt++) {
            final float angle = random.nextFloat() * (float) Math.PI * 2f;
            final float dx = (float) Math.cos(angle) * desiredDistance;
            final float dz = (float) Math.sin(angle) * desiredDistance;
            final float x = playerPos.x + dx;
            final float z = playerPos.z + dz;

            final int[] grid = maze.worldToGrid(x, z);
            if (!maze.isWall(grid[0], grid[1])) {
                enemy.getPosition().set(x, enemy.getPosition().y, z);
                final float yaw = (float) Math.toDegrees(Math.atan2(playerPos.x - x, -(playerPos.z - z)));
                enemy.setYaw(yaw);
                return;
            }
        }
    }

    private void playOnce(final Sound sound, final float volume) {
        if (sound != null) {
            sound.play(volume);
        }
    }

    private static final class ActiveEvent {
        private final HorrorEvent event;
        private float elapsed = 0f;

        ActiveEvent(final HorrorEvent event) {
            this.event = event;
        }

        HorrorEvent getEvent() {
            return event;
        }
    }
}
