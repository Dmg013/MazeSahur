package nl.saxion.game.mazesahur.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.screen.GameScreen;
import nl.saxion.game.mazesahur.net.RemotePlayerState;
import nl.saxion.gameapp.GameApp;

/**
 * Handles all UI rendering for the game.
 * Modern dark theme UI with minimal HUD elements for immersive gameplay.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class GameUI {

    private static final String FONT_NAME = "gameUI";
    private static final int FONT_SIZE = 20;

    // Energy bar dimensions
    private static final float ENERGY_BAR_WIDTH = 300f;
    private static final float ENERGY_BAR_HEIGHT = 8f;
    private static final float ENERGY_BAR_MARGIN = 30f;

    // Colors - Modern dark theme matching menu
    private static final Color PANEL_BG = new Color(0.08f, 0.08f, 0.1f, 0.85f);
    private static final Color ACCENT_RED = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color ACCENT_RED_DIM = new Color(0.7f, 0.1f, 0.1f, 0.9f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color TEXT_DIM = new Color(0.6f, 0.6f, 0.6f, 0.8f);
    private static final Color WARNING_COLOR = new Color(1.0f, 0.3f, 0.3f, 1.0f);
    private static final Color SUCCESS_COLOR = new Color(0.3f, 1.0f, 0.3f, 1.0f);
    private static final Color BOOST_COLOR = new Color(0.2f, 0.8f, 1.0f, 1.0f);
    private static final Color ESP_COLOR = new Color(1.0f, 0.5f, 0.0f, 1.0f);
    private static final Color ESP_BG = new Color(0.1f, 0.05f, 0.0f, 0.9f);

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont smallFont;
    private GlyphLayout layout;

    // Debug mode toggle
    private boolean debugMode = false;

    /**
     * Init
     */
    public void initialize() {
        System.out.println("[GameUI] Initializing modern UI...");
        GameApp.addFont(FONT_NAME, "fonts/basic.ttf", FONT_SIZE);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        font = new BitmapFont();
        font.getData().setScale(1.5f);
        font.setColor(TEXT_COLOR);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.0f);
        smallFont.setColor(TEXT_DIM);

        layout = new GlyphLayout();
        System.out.println("[GameUI] Modern UI initialized");
    }

    /**
     * Renders the game UI with modern styling.
     *
     * @param gameScreen Reference to the game screen
     * @param player The player entity
     * @param enemy The enemy entity
     * @param lightingManager The lighting manager
     * @param camera The main game camera
     * @param remotePlayers Remote players to show names for (nullable)
     */
    public void render(final GameScreen gameScreen, final Player player,
                       final Enemy enemy, final LightingManager lightingManager,
                       final PerspectiveCamera camera, final java.util.List<RemotePlayerState> remotePlayers) {
        render(gameScreen, player, enemy, lightingManager, camera, remotePlayers, false, null, 0f);
    }

    /**
     * Renders the game UI with modern styling and optional exit ESP.
     *
     * @param gameScreen Reference to the game screen
     * @param player The player entity
     * @param enemy The enemy entity
     * @param lightingManager The lighting manager
     * @param camera The main game camera
     * @param remotePlayers Remote players to show names for (nullable)
     * @param showExitESP Whether to show exit ESP indicator
     * @param exitPosition The exit position (nullable if showExitESP is false)
     * @param playerYaw The player's yaw angle for direction calculation
     */
    public void render(final GameScreen gameScreen, final Player player,
                       final Enemy enemy, final LightingManager lightingManager,
                       final PerspectiveCamera camera, final java.util.List<RemotePlayerState> remotePlayers,
                       final boolean showExitESP, final Vector3 exitPosition, final float playerYaw) {
        // Reset OpenGL state for 2D rendering
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        final int screenWidth = Gdx.graphics.getWidth();
        final int screenHeight = Gdx.graphics.getHeight();

        // Draw energy bar at bottom center
        drawModernEnergyBar(player.getEnergy(), screenWidth, screenHeight);

        batch.begin();

        // Top-left corner: Game title (minimal)
        font.setColor(ACCENT_RED);
        font.draw(batch, "MAZESAHUR", 20, screenHeight - 20);

        // Top-center: Level indicator
        font.setColor(TEXT_COLOR);
        final String levelText = "LEVEL " + gameScreen.getCurrentLevel() + " / 5";
        layout.setText(font, levelText);
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight - 20);

        // Top-right corner: Status indicators
        drawStatusIndicators(lightingManager, player, screenWidth, screenHeight);

        // Boost notification (center top)
        if (player.isBoostActive()) {
            drawBoostNotification(player, screenWidth, screenHeight);
        }

        // Warning indicator if enemy is close
        final float distance = player.getPosition().dst(enemy.getPosition());
        if (distance < 10f && enemy.getCurrentState() == Enemy.AIState.PURSUING) {
            drawDangerWarning(screenWidth, screenHeight);
        }

        // Bottom-left: Subtle controls hint
        smallFont.setColor(TEXT_DIM);
        smallFont.draw(batch, "ESC - Menu  |  F - Flashlight  |  SHIFT - Run  |  Y - Exit ESP", 20, 25);

        // Debug info (toggle with F3)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F3)) {
            debugMode = !debugMode;
        }
        if (debugMode) {
            drawDebugPanel(player, enemy, screenWidth, screenHeight);
        }

        // Draw remote player names above heads
        if (remotePlayers != null && camera != null) {
            drawPlayerNames(remotePlayers, camera, screenWidth, screenHeight);
        }

        // Draw exit ESP if enabled
        if (showExitESP && exitPosition != null) {
            drawExitESP(player.getPosition(), exitPosition, playerYaw, screenWidth, screenHeight);
        }

        batch.end();

        // Re-enable depth test for 3D rendering
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    /**
     * Draws status indicators in the top-right corner.
     */
    private void drawStatusIndicators(final LightingManager lightingManager, final Player player,
                                       final int screenWidth, final int screenHeight) {
        final float rightX = screenWidth - 20;
        float currentY = screenHeight - 20;

        // Flashlight indicator
        font.setColor(lightingManager.isFlashlightEnabled() ? SUCCESS_COLOR : TEXT_DIM);
        layout.setText(font, lightingManager.isFlashlightEnabled() ? "● LIGHT" : "○ LIGHT");
        font.draw(batch, layout, rightX - layout.width, currentY);
        currentY -= 35;

        // Energy percentage
        font.setColor(getEnergyColor(player.getEnergy()));
        final String energyText = String.format("%d%% ENERGY", (int)(player.getEnergy() * 100));
        layout.setText(font, energyText);
        font.draw(batch, layout, rightX - layout.width, currentY);
    }

    /**
     * Draws boost notification at center top.
     */
    private void drawBoostNotification(final Player player, final int screenWidth, final int screenHeight) {
        final int boostTimeRemaining = (int) Math.ceil(player.getBoostTimeRemaining());
        final String boostMultiplier = String.format("%.0f%%", (player.getSpeedMultiplier() - 1.0f) * 100f);

        // Panel background
        final float panelWidth = 280;
        final float panelHeight = 60;
        final float panelX = (screenWidth - panelWidth) / 2f;
        final float panelY = screenHeight - 100;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(BOOST_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Text
        font.setColor(BOOST_COLOR);
        final String boostText = "SPEED BOOST +" + boostMultiplier;
        layout.setText(font, boostText);
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, panelY + panelHeight - 15);

        smallFont.setColor(BOOST_COLOR);
        final String timeText = boostTimeRemaining + "s remaining";
        layout.setText(smallFont, timeText);
        smallFont.draw(batch, layout, (screenWidth - layout.width) / 2f, panelY + 20);
    }

    /**
     * Draws the exit ESP indicator showing distance and direction to the exit.
     */
    private void drawExitESP(final Vector3 playerPos, final Vector3 exitPos, 
                              final float playerYaw, final int screenWidth, final int screenHeight) {
        // Calculate distance
        final float dx = exitPos.x - playerPos.x;
        final float dz = exitPos.z - playerPos.z;
        final float distance = (float) Math.sqrt(dx * dx + dz * dz);

        // Calculate angle to exit (in degrees, 0 = north)
        float angleToExit = (float) Math.toDegrees(Math.atan2(dx, -dz));
        
        // Calculate relative angle (how much to turn from current facing)
        float relativeAngle = angleToExit - playerYaw;
        
        // Normalize to -180 to 180
        while (relativeAngle > 180) relativeAngle -= 360;
        while (relativeAngle < -180) relativeAngle += 360;

        // Panel dimensions
        final float panelWidth = 200;
        final float panelHeight = 120;
        final float panelX = screenWidth - panelWidth - 20;
        final float panelY = screenHeight / 2f - panelHeight / 2f;

        // Draw panel background
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(ESP_BG);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Border with pulsing effect
        final float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 200.0);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shapeRenderer.setColor(ESP_COLOR.r * pulse, ESP_COLOR.g * pulse, ESP_COLOR.b, 1f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Draw compass arrow
        final float compassCenterX = panelX + panelWidth / 2f;
        final float compassCenterY = panelY + panelHeight - 45;
        final float arrowLength = 30f;

        // Convert relative angle to radians for drawing
        final float arrowAngleRad = (float) Math.toRadians(-relativeAngle + 90);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(ESP_COLOR);

        // Arrow tip
        final float tipX = compassCenterX + (float) Math.cos(arrowAngleRad) * arrowLength;
        final float tipY = compassCenterY + (float) Math.sin(arrowAngleRad) * arrowLength;

        // Arrow base points
        final float baseAngle1 = arrowAngleRad + (float) Math.toRadians(140);
        final float baseAngle2 = arrowAngleRad - (float) Math.toRadians(140);
        final float baseLength = arrowLength * 0.6f;

        final float base1X = compassCenterX + (float) Math.cos(baseAngle1) * baseLength;
        final float base1Y = compassCenterY + (float) Math.sin(baseAngle1) * baseLength;
        final float base2X = compassCenterX + (float) Math.cos(baseAngle2) * baseLength;
        final float base2Y = compassCenterY + (float) Math.sin(baseAngle2) * baseLength;

        shapeRenderer.triangle(tipX, tipY, base1X, base1Y, base2X, base2Y);

        // Center circle
        shapeRenderer.setColor(ESP_BG);
        shapeRenderer.circle(compassCenterX, compassCenterY, 8);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(ESP_COLOR);
        shapeRenderer.circle(compassCenterX, compassCenterY, 8);
        shapeRenderer.end();

        // Resume batch for text
        batch.begin();

        // Title
        font.setColor(ESP_COLOR);
        layout.setText(font, "EXIT ESP");
        font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, panelY + panelHeight - 5);

        // Distance text
        font.setColor(1f, 1f, 1f, 1f);
        final String distanceText = String.format("%.0f m", distance);
        layout.setText(font, distanceText);
        font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, panelY + 35);

        // Direction hint
        smallFont.setColor(TEXT_DIM);
        String directionHint;
        if (Math.abs(relativeAngle) < 15) {
            directionHint = "STRAIGHT AHEAD";
            smallFont.setColor(SUCCESS_COLOR);
        } else if (relativeAngle > 0) {
            directionHint = "TURN RIGHT";
        } else {
            directionHint = "TURN LEFT";
        }
        layout.setText(smallFont, directionHint);
        smallFont.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, panelY + 15);
    }

    /**
     * Draws danger warning when enemy is pursuing and close.
     */
    private void drawDangerWarning(final int screenWidth, final int screenHeight) {
        // Pulsing warning text
        final float pulse = (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0));
        font.setColor(WARNING_COLOR.r, WARNING_COLOR.g, WARNING_COLOR.b, pulse);

        final String warningText = "! DANGER !";
        layout.setText(font, warningText);
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight / 2 + 200);
    }

    /**
     * Draws debug information panel.
     */
    private void drawDebugPanel(final Player player, final Enemy enemy,
                                 final int screenWidth, final int screenHeight) {
        final float panelWidth = 300;
        final float panelHeight = 180;
        final float panelX = 20;
        final float panelY = screenHeight - 200;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(1f);
        shapeRenderer.setColor(ACCENT_RED_DIM);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Debug text
        float textY = panelY + panelHeight - 15;
        smallFont.setColor(ACCENT_RED);
        smallFont.draw(batch, "DEBUG INFO (F3 to hide)", panelX + 10, textY);
        textY -= 25;

        smallFont.setColor(TEXT_COLOR);
        final float distance = player.getPosition().dst(enemy.getPosition());
        smallFont.draw(batch, "Enemy Distance: " + (int)distance + "m", panelX + 10, textY);
        textY -= 20;

        smallFont.draw(batch, "Enemy State: " + enemy.getCurrentState(), panelX + 10, textY);
        textY -= 20;

        if (enemy.getCurrentState() == Enemy.AIState.PURSUING) {
            final int timeRemaining = (int) (GameConfig.ENEMY_CHASE_MEMORY_DURATION
                - enemy.getTimeSincePlayerSeen());
            smallFont.draw(batch, "Pursuit Time: " + timeRemaining + "s", panelX + 10, textY);
            textY -= 20;
        }

        final Vector3 pos = player.getPosition();
        smallFont.draw(batch, String.format("Position: (%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z),
            panelX + 10, textY);
        textY -= 20;

        smallFont.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), panelX + 10, textY);
    }

    /**
     * Draws player names above their heads.
     */
    private void drawPlayerNames(final java.util.List<RemotePlayerState> remotePlayers,
                                  final PerspectiveCamera camera,
                                  final int screenWidth, final int screenHeight) {
        final Vector3 world = new Vector3();
        final Vector3 screen = new Vector3();

        for (RemotePlayerState rp : remotePlayers) {
            if (rp == null || rp.name == null) continue;

            world.set(rp.x, rp.y + GameConfig.PLAYER_HEIGHT + 0.5f, rp.z);
            screen.set(world);
            camera.project(screen);

            // Only draw if in front of camera and on screen
            if (screen.z > 0 && screen.x >= 0 && screen.x <= screenWidth
                && screen.y >= 0 && screen.y <= screenHeight) {
                final float drawX = screen.x;
                final float drawY = screenHeight - screen.y;

                // Background for name
                layout.setText(smallFont, rp.name);
                final float bgWidth = layout.width + 10;
                final float bgHeight = layout.height + 6;

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(PANEL_BG);
                shapeRenderer.rect(drawX - bgWidth / 2, drawY - bgHeight / 2, bgWidth, bgHeight);
                shapeRenderer.end();

                // Name text
                smallFont.setColor(TEXT_COLOR);
                smallFont.draw(batch, rp.name, drawX - layout.width / 2, drawY + layout.height / 2);
            }
        }
    }

    /**
     * Draws the modern energy bar at the bottom center.
     */
    private void drawModernEnergyBar(final float energy, final int screenWidth, final int screenHeight) {
        final float barX = (screenWidth - ENERGY_BAR_WIDTH) / 2f;
        final float barY = ENERGY_BAR_MARGIN;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background (dark)
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.7f);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);

        // Energy fill with color based on level
        final Color energyColor = getEnergyColor(energy);
        shapeRenderer.setColor(energyColor);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH * energy, ENERGY_BAR_HEIGHT);

        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(ACCENT_RED_DIM);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
        shapeRenderer.end();
    }

    /**
     * Gets the color for the energy bar based on current energy level.
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
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (smallFont != null) {
            smallFont.dispose();
        }
    }
}
