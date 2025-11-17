package nl.saxion.game.mazesahur.ui;

import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.entity.Elevator;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.screen.GameScreen;
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

    /**
     * Init
     */
    public void initialize() {
        System.out.println("[GameUI] Initializing UI and loading font...");
        GameApp.addFont(FONT_NAME, "fonts/basic.ttf", FONT_SIZE);
        System.out.println("[GameUI] Font loaded: " + FONT_NAME);
    }

    /**
     * Renders the game UI.
     *
     * @param gameScreen Reference to the game screen
     * @param player The player entity
     * @param enemy The enemy entity
     * @param elevator The elevator entity
     * @param lightingManager The lighting manager
     */
    public void render(final GameScreen gameScreen, final Player player,
                       final Enemy enemy, final Elevator elevator, final LightingManager lightingManager) {
        // Reset OpenGL state for 2D rendering
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(
            com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
            com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        GameApp.startSpriteRendering();

        // Use actual window height - but remember GameApp uses different coordinate system
        final int screenHeight = com.badlogic.gdx.Gdx.graphics.getHeight();

        // Title (at top)
        GameApp.drawText(FONT_NAME, "MazeSahur - Horror Maze Game",
            20, 20, "white");

        // Controls
        GameApp.drawText(FONT_NAME, "WASD: Move | Mouse: Look | F: Flashlight | ESC: Exit",
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

        // Exit hint (at bottom)
        GameApp.drawText(FONT_NAME, "ESC to exit", 20, screenHeight - 30, "amber-500");

        GameApp.endSpriteRendering();

        // Re-enable depth test for 3D rendering
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
    }

    /**
     * Disposes UI resources.
     */
    public void dispose() {
        GameApp.disposeFont(FONT_NAME);
    }
}

