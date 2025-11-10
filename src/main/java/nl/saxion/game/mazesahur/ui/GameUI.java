package nl.saxion.game.mazesahur.ui;

import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.screen.GameScreen;
import nl.saxion.gameapp.GameApp;

/**
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
     * Initializes UI resources (fonts, etc.).
     */
    public void initialize() {
        GameApp.addFont(FONT_NAME, "fonts/basic.ttf", FONT_SIZE);
    }

    /**
     * Renders the game UI.
     *
     * @param gameScreen Reference to the game screen
     * @param player The player entity
     * @param enemy The enemy entity
     * @param lightingManager The lighting manager
     */
    public void render(final GameScreen gameScreen, final Player player,
                       final Enemy enemy, final LightingManager lightingManager) {
        GameApp.startSpriteRendering();

        // Title
        GameApp.drawText(FONT_NAME, "MazeSahur - Horror Maze Game",
            20, gameScreen.getWorldHeight() - 20, "white");

        // Controls
        GameApp.drawText(FONT_NAME, "WASD: Move | Mouse: Look | F: Flashlight | ESC: Exit",
            20, gameScreen.getWorldHeight() - 50, "white");

        // Flashlight status
        final String flashlightStatus = lightingManager.isFlashlightEnabled() ? "ON" : "OFF";
        final String flashlightColor = lightingManager.isFlashlightEnabled() ? "green-500" : "red-500";
        GameApp.drawText(FONT_NAME, "Flashlight: " + flashlightStatus,
            20, gameScreen.getWorldHeight() - 80, flashlightColor);

        // Enemy info
        final float distance = player.getPosition().dst(enemy.getPosition());
        GameApp.drawText(FONT_NAME, "Enemy distance: " + (int)distance + "m",
            20, gameScreen.getWorldHeight() - 110, "red-500");
        GameApp.drawText(FONT_NAME, "Enemy state: " + enemy.getCurrentState(),
            20, gameScreen.getWorldHeight() - 140, "amber-500");

        // Pursuit timer
        if (enemy.getCurrentState() == Enemy.AIState.PURSUING) {
            final int timeRemaining = (int) (GameConfig.ENEMY_CHASE_MEMORY_DURATION
                - enemy.getTimeSincePlayerSeen());
            GameApp.drawText(FONT_NAME, "Pursuit time: " + timeRemaining + "s",
                20, gameScreen.getWorldHeight() - 170, "red-500");
        }

        // Exit hint
        GameApp.drawText(FONT_NAME, "ESC to exit", 20, 30, "amber-500");

        GameApp.endSpriteRendering();
    }

    /**
     * Disposes UI resources.
     */
    public void dispose() {
        GameApp.disposeFont(FONT_NAME);
    }
}

