package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Reusable loading screen with the same visual style as the startup splash.
 */
public class LoadingScreen extends ScalableGameScreen {

    private static final int BAR_WIDTH = 700;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_BOTTOM_MARGIN = 40;
    private static final float DEFAULT_MIN_DISPLAY_TIME = 1.0f;
    private static final float START_DELAY = 0.05f;

    private final String loadingText;
    private final float minDisplayTime;
    private final Runnable loadAction;
    private final Runnable onComplete;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture splashImage;
    private final Matrix4 projection = new Matrix4();

    private float elapsedTime = 0f;
    private boolean loadStarted = false;
    private boolean loadFinished = false;
    private boolean transitionQueued = false;

    public LoadingScreen(final String loadingText,
                         final float minDisplayTime,
                         final Runnable loadAction,
                         final Runnable onComplete) {
        super(1280, 720);
        this.loadingText = loadingText != null ? loadingText : "Loading...";
        this.minDisplayTime = Math.max(0.1f, minDisplayTime);
        this.loadAction = loadAction;
        this.onComplete = onComplete;
    }

    public LoadingScreen(final String loadingText,
                         final Runnable loadAction,
                         final Runnable onComplete) {
        this(loadingText, DEFAULT_MIN_DISPLAY_TIME, loadAction, onComplete);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        splashImage = new Texture(Gdx.files.internal("img/splash.png"));
    }

    @Override
    public void render(final float delta) {
        elapsedTime += delta;

        final int screenWidth = Gdx.graphics.getBackBufferWidth();
        final int screenHeight = Gdx.graphics.getBackBufferHeight();

        // Ensure full-window viewport and matching projection
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        projection.setToOrtho2D(0, 0, screenWidth, screenHeight);
        batch.setProjectionMatrix(projection);
        shapeRenderer.setProjectionMatrix(projection);

        if (!loadStarted && elapsedTime >= START_DELAY) {
            loadStarted = true;
            if (loadAction != null) {
                loadAction.run();
            }
            loadFinished = true;
        }

        if (!transitionQueued && loadFinished && elapsedTime >= minDisplayTime) {
            transitionQueued = true;
            if (onComplete != null) {
                Gdx.app.postRunnable(onComplete);
            }
        }

        if (transitionQueued) {
            return;
        }

        // Clear screen
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw splash image fullscreen
        batch.begin();
        batch.draw(splashImage, 0, 0, screenWidth, screenHeight);
        batch.end();

        // Draw loading bar
        final int barX = (screenWidth - BAR_WIDTH) / 2;
        final int barY = BAR_BOTTOM_MARGIN;
        final float progress = loadFinished
            ? 1.0f
            : Math.min(0.95f, elapsedTime / minDisplayTime);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1.0f);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        shapeRenderer.rect(barX, barY, BAR_WIDTH * progress, BAR_HEIGHT);
        shapeRenderer.end();

        // Draw loading text
        batch.begin();
        font.setColor(Color.WHITE);
        final int textY = barY + BAR_HEIGHT + 25;
        font.draw(batch, loadingText, 0, textY, screenWidth, Align.center, false);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (splashImage != null) splashImage.dispose();
    }

    @Override
    public void hide() {
        // No-op
    }
}
