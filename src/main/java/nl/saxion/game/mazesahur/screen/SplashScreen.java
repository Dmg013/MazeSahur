package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;


/**
 * Splash screen with loading bar and progress text.
 * Shows centered window with game logo and loading progress.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class SplashScreen extends ScalableGameScreen {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture splashImage;

    // Loading bar dimensions and position
    private static final int BAR_WIDTH = 700;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_BOTTOM_MARGIN = 40; // Pixels from bottom of window

    // Loading stages
    private static final String[] LOADING_STAGES = {
        "Initializing...",
        "Loading resources...",
        "Preparing menu...",
        "Finalizing..."
    };

    private volatile int currentStage = 0;
    private float stageTimer = 0.0f;
    private static final float MINIMUM_DISPLAY_TIME = 2.0f; // Minimum 2 seconds display
    private float elapsedTime = 0.0f;
    private volatile boolean menuLoaded = false;
    private boolean readyToTransition = false;
    private boolean transitionStarted = false;

    /**
     * Creates a new splash screen.
     */
    public SplashScreen() {
        super(900, 500);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);

        // Load splash image
        splashImage = new Texture(Gdx.files.internal("img/splash.png"));

        // Try to make window borderless, centered, and always on top
        try {
            // Set window to undecorated (borderless)
            Gdx.graphics.setUndecorated(true);

            // Try to get the LWJGL3 window for always on top
            try {
                // Use reflection to access LWJGL3 window
                final Object lwjgl3App = Gdx.app;
                final java.lang.reflect.Method getWindowMethod =
                    lwjgl3App.getClass().getMethod("getWindow");
                final Object window = getWindowMethod.invoke(lwjgl3App);

                final java.lang.reflect.Method getWindowHandleMethod =
                    window.getClass().getMethod("getWindowHandle");
                final long windowHandle = (long) getWindowHandleMethod.invoke(window);

                // Set always on top using GLFW
                org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(windowHandle,
                    org.lwjgl.glfw.GLFW.GLFW_FLOATING, org.lwjgl.glfw.GLFW.GLFW_TRUE);

                System.out.println("[SplashScreen] Window set to always on top");
            } catch (Exception e) {
                System.out.println("[SplashScreen] Could not set always on top: " + e.getMessage());
            }

            // Center window on screen
            Gdx.graphics.setWindowedMode(900, 500);

            System.out.println("[SplashScreen] Window set to borderless and centered");
        } catch (Exception e) {
            System.out.println("[SplashScreen] Could not set borderless mode: " + e.getMessage());
        }

        System.out.println("[SplashScreen] Displayed - game will load on render thread");
    }

    @Override
    public void render(float delta) {
        // Update loading progress
        elapsedTime += delta;
        stageTimer += delta;

        // Progress through loading stages
        if (!menuLoaded) {
            // Auto-progress through stages
            if (currentStage == 0) {
                System.out.println("[SplashScreen] Stage 0: Initializing...");
                currentStage = 1;
                stageTimer = 0.0f;
            } else if (currentStage == 1 && stageTimer > 0.3f) {
                System.out.println("[SplashScreen] Stage 1: Loading resources...");
                currentStage = 2;
                stageTimer = 0.0f;
            } else if (currentStage == 2 && stageTimer > 0.3f) {
                System.out.println("[SplashScreen] Stage 2: Preparing menu...");
                currentStage = 3;
                stageTimer = 0.0f;
            } else if (currentStage == 3 && stageTimer > 0.3f) {
                System.out.println("[SplashScreen] Stage 3: Finalizing");
                menuLoaded = true;
            }
        }

        // Calculate progress based on current stage
        float loadingProgress = (float) (currentStage + 1) / LOADING_STAGES.length;

        // Check if we can transition (minimum display time + menu loaded)
        if (elapsedTime >= MINIMUM_DISPLAY_TIME && menuLoaded) {
            readyToTransition = true;
        }

        // Transition to menu
        if (readyToTransition && !transitionStarted) {
            transitionStarted = true;
            System.out.println("[SplashScreen] Loading complete, transitioning to menu...");

            // Schedule transition on next frame
            Gdx.app.postRunnable(() -> {
                System.out.println("[SplashScreen] Executing transition...");
                // Restore window decorations and resize for menu
                Gdx.graphics.setUndecorated(false);
                Gdx.graphics.setWindowedMode(1280, 720);

                // Create and add menu screen
                GameApp.addScreen("Menu", new MenuScreen());

                // Switch to menu screen
                GameApp.switchScreen("Menu");
            });
        }

        // Don't render anything if transition started
        if (transitionStarted) {
            return;
        }

        // Clear screen (not visible since splash fills entire window)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Draw splash image fullscreen (fills entire 900x500 window)
        batch.begin();
        batch.draw(splashImage, 0, 0, screenWidth, screenHeight);
        batch.end();

        // Draw loading bar at bottom center
        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = BAR_BOTTOM_MARGIN;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Bar background
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1.0f);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);

        // Bar progress (white)
        shapeRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        shapeRenderer.rect(barX, barY, BAR_WIDTH * loadingProgress, BAR_HEIGHT);

        shapeRenderer.end();

        // Draw loading text above loading bar
        batch.begin();
        String loadingText = LOADING_STAGES[currentStage];
        font.setColor(Color.WHITE);

        // Center text above loading bar
        int textY = barY + BAR_HEIGHT + 25;
        font.draw(batch, loadingText,
            0, textY,
            screenWidth, Align.center, false);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Handle resize if needed
    }

    @Override
    public void pause() {
        // Not needed
    }

    @Override
    public void resume() {
        // Not needed
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (splashImage != null) splashImage.dispose();
    }
}
