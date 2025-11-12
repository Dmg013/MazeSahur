package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Death screen displayed when the player is caught by the enemy.
 * Shows game over message, survival statistics, and restart option.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class DeathScreen extends ScalableGameScreen {

    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont normalFont;

    private float fadeAlpha;
    private float survivalTime;
    private boolean allowRestart;
    private float displayTimer;

    /**
     * Creates a new death screen.
     *
     * @param survivalTime How long the player survived in seconds
     */
    public DeathScreen(final float survivalTime) {
        super(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        this.survivalTime = survivalTime;
        this.fadeAlpha = 0f;
        this.allowRestart = false;
        this.displayTimer = 0f;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Create fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.0f);
        titleFont.setColor(Color.RED);

        normalFont = new BitmapFont();
        normalFont.getData().setScale(1.5f);
        normalFont.setColor(Color.WHITE);
    }

    @Override
    public void render(final float delta) {
        displayTimer += delta;

        // Fade in effect
        if (fadeAlpha < 1.0f) {
            fadeAlpha = Math.min(1.0f, fadeAlpha + delta / GameConfig.DEATH_SCREEN_FADE_DURATION);
        }

        // Allow restart after fade completes
        if (fadeAlpha >= 1.0f && !allowRestart) {
            allowRestart = true;
        }

        // Clear screen to black
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render UI
        batch.begin();

        // Apply fade alpha to all text
        titleFont.setColor(Color.RED.r, Color.RED.g, Color.RED.b, fadeAlpha);
        normalFont.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, fadeAlpha);

        final int screenWidth = Gdx.graphics.getWidth();
        final int screenHeight = Gdx.graphics.getHeight();

        // Title: "YOU WERE CAUGHT"
        final String title = "YOU WERE CAUGHT";
        titleFont.draw(batch, title, 0, screenHeight * 0.6f, screenWidth, Align.center, false);

        // Subtitle with skull emoji
        final String subtitle = "SAHUR GOT YOU";
        normalFont.draw(batch, subtitle, 0, screenHeight * 0.5f, screenWidth, Align.center, false);

        // Survival time
        final int minutes = (int) (survivalTime / 60);
        final int seconds = (int) (survivalTime % 60);
        final String timeText = String.format("Survival Time: %02d:%02d", minutes, seconds);
        normalFont.draw(batch, timeText, 0, screenHeight * 0.4f, screenWidth, Align.center, false);

        // Restart prompt (blinking effect)
        if (allowRestart) {
            final float blinkAlpha = (float) (Math.sin(displayTimer * 3.0) * 0.5 + 0.5);
            normalFont.setColor(1f, 1f, 1f, blinkAlpha * fadeAlpha);
            final String restartText = "Press SPACE to Restart";
            normalFont.draw(batch, restartText, 0, screenHeight * 0.25f, screenWidth, Align.center, false);
        }

        batch.end();

        // Handle restart input
        if (allowRestart && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            restart();
        }

        // Allow ESC to exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    /**
     * Restarts the game by creating a new GameScreen.
     */
    private void restart() {
        System.out.println("[DeathScreen] Restarting game...");

        // Create new game screen
        final GameScreen newGameScreen = new GameScreen();

        // Initialize the new game screen
        newGameScreen.show();

        // Register and switch to new game screen (GameApp handles disposing old screen)
        GameApp.addScreen("MazeGame", newGameScreen);
        GameApp.switchScreen("MazeGame");
    }

    @Override
    public void resize(final int width, final int height) {
        // No resize handling needed for death screen
    }

    @Override
    public void hide() {
        // Called when screen is hidden
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (normalFont != null) normalFont.dispose();
    }
}
