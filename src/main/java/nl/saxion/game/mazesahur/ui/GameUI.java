package nl.saxion.game.mazesahur.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.entity.Elevator;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.screen.GameScreen;
import nl.saxion.game.mazesahur.net.RemotePlayerState;
import nl.saxion.gameapp.GameApp;

/**w=
 * Handles all UI rendering for the game.
 * Displays HUD information, controls, and debug info.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class GameUI {

    private static final String FONT_NAME = "gameUI";
    private static final int FONT_SIZE = 24;

    // Energy bar dimensions
    private static final float ENERGY_BAR_WIDTH = 300f;
    private static final float ENERGY_BAR_HEIGHT = 30f;
    private static final float ENERGY_BAR_MARGIN = 20f;

    private ShapeRenderer shapeRenderer;

    /**
     * Init
     */
    public void initialize() {
        System.out.println("[GameUI] Initializing UI and loading font...");
        GameApp.addFont(FONT_NAME, "fonts/basic.ttf", FONT_SIZE);
        shapeRenderer = new ShapeRenderer();
        System.out.println("[GameUI] Font loaded: " + FONT_NAME);
    }

    /**
     * Renders the game UI.f
     *
     * @param gameScreen Reference to the game screen
     * @param player The player entity
     * @param enemy The enemy entity
     * @param elevator The elevator entity
     * @param lightingManager The lighting manager
     * @param camera The main game camera
     * @param remotePlayers Remote players to show names for (nullable)
     */
    public void render(final GameScreen gameScreen, final Player player,
                       final Enemy enemy, final Elevator elevator, final LightingManager lightingManager,
                       final PerspectiveCamera camera, final java.util.List<RemotePlayerState> remotePlayers) {
        // Reset OpenGL state for 2D rendering
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(
            com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
            com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        // Use actual window dimensions
        final int screenWidth = com.badlogic.gdx.Gdx.graphics.getWidth();
        final int screenHeight = com.badlogic.gdx.Gdx.graphics.getHeight();

        // Draw energy bar at bottom center
        drawEnergyBar(player.getEnergy(), screenWidth, screenHeight);

        GameApp.startSpriteRendering();

        // Title (at top)
        GameApp.drawText(FONT_NAME, "MazeSahur - Horror Maze Game",
            20, 20, "white");

        // Controls
        GameApp.drawText(FONT_NAME, "WASD: Move | Shift: Run | Mouse: Look | F: Flashlight | ESC: Exit",
            20, 50, "white");

        // Flashlight status
        final String flashlightStatus = lightingManager.isFlashlightEnabled() ? "ON" : "OFF";
        final String flashlightColor = lightingManager.isFlashlightEnabled() ? "green-500" : "red-500";
        GameApp.drawText(FONT_NAME, "Flashlight: " + flashlightStatus,
            20, 80, flashlightColor);

        // Enemy info
        final float distance = player.getPosition().dst(enemy.getPosition());
        GameApp.drawText(FONT_NAME, "Enemy distance: " + (int)distance + "m",
            20, 110, "red-500");
        GameApp.drawText(FONT_NAME, "Enemy state: " + enemy.getCurrentState(),
            20, 140, "amber-500");

        // Pursuit timer
        if (enemy.getCurrentState() == Enemy.AIState.PURSUING) {
            final int timeRemaining = (int) (GameConfig.ENEMY_CHASE_MEMORY_DURATION
                - enemy.getTimeSincePlayerSeen());
            GameApp.drawText(FONT_NAME, "Pursuit time: " + timeRemaining + "s",
                20, 170, "red-500");
        }

        // Elevator info (distance tracker like Sahur)
        final float elevatorDistance = elevator.getDistanceToPlayer(player.getPosition());
        GameApp.drawText(FONT_NAME, "Elevator distance: " + (int)elevatorDistance + "m",
            20, 200, "blue-500");

        // Elevator state
        String elevatorStateText = "Elevator: " + elevator.getCurrentState();
        String elevatorStateColor = "gray-500";
        if (elevator.getCurrentState() == Elevator.ElevatorState.OPEN) {
            elevatorStateColor = "green-500";
        } else if (elevator.getCurrentState() == Elevator.ElevatorState.OPENING
                   || elevator.getCurrentState() == Elevator.ElevatorState.CLOSING) {
            elevatorStateColor = "amber-500";
        }
        GameApp.drawText(FONT_NAME, elevatorStateText, 20, 230, elevatorStateColor);

        // Boost status (if active)
        if (player.isBoostActive()) {
            final int boostTimeRemaining = (int) Math.ceil(player.getBoostTimeRemaining());
            final String boostMultiplier = String.format("%.0f%%", (player.getSpeedMultiplier() - 1.0f) * 100f);
            GameApp.drawText(FONT_NAME, "SPEED BOOST: +" + boostMultiplier + " (" + boostTimeRemaining + "s)",
                20, 260, "cyan-500");
        }

        // Exit hint (at bottom)
        GameApp.drawText(FONT_NAME, "ESC to exit", 20, screenHeight - 30, "amber-500");

        // Draw remote player names above heads
        if (remotePlayers != null && camera != null) {
            final Vector3 world = new Vector3();
            final Vector3 screen = new Vector3();
            for (RemotePlayerState rp : remotePlayers) {
                if (rp == null || rp.name == null) continue;
                world.set(rp.x, rp.y + GameConfig.PLAYER_HEIGHT + 0.5f, rp.z);
                screen.set(world);
                camera.project(screen);
                // Only draw if in front of camera and on screen
                if (screen.z > 0 && screen.x >= 0 && screen.x <= screenWidth && screen.y >= 0 && screen.y <= screenHeight) {
                    final float drawX = screen.x;
                    final float drawY = screenHeight - screen.y; // Convert to UI coord system
                    GameApp.drawText(FONT_NAME, rp.name, drawX, drawY, "white");
                }
            }
        }

        GameApp.endSpriteRendering();

        // Re-enable depth test for 3D rendering
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
    }

    /**
     * Draws the energy bar at the bottom center of the screen.
     *
     * @param energy Energy level (0.0 to 1.0)
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     */
    private void drawEnergyBar(final float energy, final int screenWidth, final int screenHeight) {
        // Calculate position (bottom center)
        final float barX = (screenWidth - ENERGY_BAR_WIDTH) / 2f;
        final float barY = ENERGY_BAR_MARGIN;

        // Enable blending for smooth colors
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw background (dark gray)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);

        // Draw energy fill with color gradient based on energy level
        final Color energyColor = getEnergyColor(energy);
        shapeRenderer.setColor(energyColor);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH * energy, ENERGY_BAR_HEIGHT);

        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1.0f);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
        shapeRenderer.end();
    }

    /**
     * Gets the color for the energy bar based on current energy level.
     *
     * @param energy Energy level (0.0 to 1.0)
     * @return Color for the energy bar
     */
    private Color getEnergyColor(final float energy) {
        if (energy > 0.5f) {
            // Green to yellow (high energy)
            final float t = (1.0f - energy) * 2f;
            return new Color(t, 1.0f, 0.0f, 0.9f);
        } else if (energy > 0.25f) {
            // Yellow to orange (medium energy)
            final float t = (0.5f - energy) * 4f;
            return new Color(1.0f, 1.0f - t * 0.5f, 0.0f, 0.9f);
        } else {
            // Orange to red (low energy)
            final float t = (0.25f - energy) * 4f;
            return new Color(1.0f, 0.5f - t * 0.5f, 0.0f, 0.9f);
        }
    }

    /**
     * Disposes UI resources.
     */
    public void dispose() {
        GameApp.disposeFont(FONT_NAME);
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
