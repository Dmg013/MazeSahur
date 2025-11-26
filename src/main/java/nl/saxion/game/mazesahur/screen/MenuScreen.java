package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Main menu screen with Play, Settings, and Multiplayer buttons.
 * Enhanced with modern UI design, animations, and visual effects.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class MenuScreen extends ScalableGameScreen {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont subtitleFont;
    private GlyphLayout glyphLayout;

    // Button dimensions and positions (as percentages of screen size)
    private static final float BUTTON_WIDTH_PERCENT = 0.31f;  // 31% of screen width
    private static final float BUTTON_HEIGHT_PERCENT = 0.097f; // ~9.7% of screen height
    private static final float BUTTON_SPACING_PERCENT = 0.035f; // 3.5% of screen height
    private static final int BUTTON_BORDER_WIDTH = 3;

    // Actual button dimensions (calculated based on screen size)
    private int buttonWidth;
    private int buttonHeight;
    private int buttonSpacing;

    // Buttons (using Rectangle for hit detection)
    private Rectangle playButton;
    private Rectangle settingsButton;
    private Rectangle multiplayerButton;

    // Animation and timing
    private float animationTime = 0f;
    private float titlePulseTime = 0f;
    private float buttonAnimationDelay = 0f;
    private boolean animationsComplete = false;

    // Font scales (calculated based on screen size)
    private float titleScale;
    private float buttonFontScale;
    private float subtitleScale;

    // Virtual viewport dimensions
    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;

    // Colors - Modern dark theme with red accents
    private static final Color BUTTON_COLOR = new Color(0.12f, 0.12f, 0.15f, 0.95f);
    private static final Color BUTTON_HOVER_COLOR = new Color(0.5f, 0.05f, 0.05f, 0.95f);
    private static final Color BUTTON_BORDER_COLOR = new Color(0.7f, 0.1f, 0.1f, 1.0f);
    private static final Color BUTTON_BORDER_HOVER = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color SUBTITLE_COLOR = new Color(0.6f, 0.6f, 0.6f, 0.8f);
    private static final Color SHADOW_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.6f);

    /**
     * Creates a new menu screen.
     */
    public MenuScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        // Create fonts
        titleFont = new BitmapFont();
        titleFont.setColor(TITLE_COLOR);

        buttonFont = new BitmapFont();
        buttonFont.setColor(Color.WHITE);

        subtitleFont = new BitmapFont();
        subtitleFont.setColor(SUBTITLE_COLOR);

        // Initialize responsive layout
        calculateResponsiveLayout();

        System.out.println("[MenuScreen] Enhanced main menu initialized");
    }

    /**
     * Calculates responsive layout based on virtual viewport dimensions.
     */
    private void calculateResponsiveLayout() {
        // Use virtual viewport dimensions for consistent positioning
        int screenWidth = VIEWPORT_WIDTH;
        int screenHeight = VIEWPORT_HEIGHT;

        // Calculate button dimensions based on screen size
        buttonWidth = (int) (screenWidth * BUTTON_WIDTH_PERCENT);
        buttonHeight = (int) (screenHeight * BUTTON_HEIGHT_PERCENT);
        buttonSpacing = (int) (screenHeight * BUTTON_SPACING_PERCENT);

        // Use fixed font scales since we're using a fixed virtual viewport
        titleScale = 5.0f;
        buttonFontScale = 2.2f;
        subtitleScale = 1.3f;

        // Apply font scales
        titleFont.getData().setScale(titleScale);
        buttonFont.getData().setScale(buttonFontScale);
        subtitleFont.getData().setScale(subtitleScale);

        // Calculate button positions (centered on screen)
        int centerX = (screenWidth - buttonWidth) / 2;
        int startY = screenHeight / 2 + 20;

        if (playButton == null) {
            playButton = new Rectangle();
            settingsButton = new Rectangle();
            multiplayerButton = new Rectangle();
        }

        playButton.set(centerX, startY, buttonWidth, buttonHeight);
        settingsButton.set(centerX, startY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight);
        multiplayerButton.set(centerX, startY - 2 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // Layout uses fixed virtual viewport dimensions, no need to recalculate
    }

    @Override
    public void render(final float delta) {
        super.render(delta);

        // Update animations
        animationTime += delta;
        titlePulseTime += delta;

        if (buttonAnimationDelay < 0.5f) {
            buttonAnimationDelay += delta;
        } else if (!animationsComplete) {
            animationsComplete = true;
        }

        // Clear screen with black background
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Get mouse position (convert from screen to viewport coordinates)
        int mouseX = Gdx.input.getX();
        int mouseY = VIEWPORT_HEIGHT - Gdx.input.getY();

        // Check hover states
        boolean playHovered = playButton.contains(mouseX, mouseY);
        boolean settingsHovered = settingsButton.contains(mouseX, mouseY);
        boolean multiplayerHovered = multiplayerButton.contains(mouseX, mouseY);

        // Handle clicks
        if (Gdx.input.justTouched()) {
            if (playHovered) {
                onPlayClicked();
            } else if (settingsHovered) {
                onSettingsClicked();
            } else if (multiplayerHovered) {
                onMultiplayerClicked();
            }
        }

        // Enable blending for transparency effects
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


        // Draw buttons with shadows and animations
        float playScale = animationsComplete ? (playHovered ? 1.05f : 1.0f) :
                          Interpolation.elasticOut.apply(0f, 1f, Math.min(1f, animationTime * 2f));
        float settingsScale = animationsComplete ? (settingsHovered ? 1.05f : 1.0f) :
                              Interpolation.elasticOut.apply(0f, 1f, Math.min(1f, (animationTime - 0.1f) * 2f));
        float multiplayerScale = animationsComplete ? (multiplayerHovered ? 1.05f : 1.0f) :
                                 Interpolation.elasticOut.apply(0f, 1f, Math.min(1f, (animationTime - 0.2f) * 2f));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw Play button
        drawButtonWithShadow(playButton, playHovered, playScale);

        // Draw Settings button
        drawButtonWithShadow(settingsButton, settingsHovered, settingsScale);

        // Draw Multiplayer button
        drawButtonWithShadow(multiplayerButton, multiplayerHovered, multiplayerScale);

        shapeRenderer.end();

        // Draw button borders with glow effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(BUTTON_BORDER_WIDTH);

        drawButtonBorder(playButton, playHovered, playScale);
        drawButtonBorder(settingsButton, settingsHovered, settingsScale);
        drawButtonBorder(multiplayerButton, multiplayerHovered, multiplayerScale);

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw text with effects
        batch.begin();

        // Use virtual viewport dimensions for consistent positioning
        int screenWidth = VIEWPORT_WIDTH;
        int screenHeight = VIEWPORT_HEIGHT;

        // Draw title with pulse effect
        float titlePulse = 1.0f + MathUtils.sin(titlePulseTime * 2f) * 0.05f;
        titleFont.getData().setScale(titleScale * titlePulse);

        // Title positions
        float titleY = screenHeight - 100;
        float titleShadowOffset = 6;

        // Title shadow
        titleFont.setColor(SHADOW_COLOR);
        titleFont.draw(batch, "MAZE SAHUR", titleShadowOffset, titleY + titleShadowOffset, screenWidth, Align.center, false);

        // Title main
        titleFont.setColor(TITLE_COLOR);
        titleFont.draw(batch, "MAZE SAHUR", 0, titleY, screenWidth, Align.center, false);

        // Subtitle
        float subtitleY = screenHeight - 170;
        subtitleFont.draw(batch, "A Horror Maze Experience",
                0, subtitleY, screenWidth, Align.center, false);

        // Draw button text with scaling
        drawButtonText("PLAY", playButton, playScale);
        drawButtonText("SETTINGS", settingsButton, settingsScale);
        drawButtonText("MULTIPLAYER", multiplayerButton, multiplayerScale);

        // Draw footer text
        subtitleFont.getData().setScale(0.9f);
        subtitleFont.setColor(new Color(0.4f, 0.4f, 0.4f, 0.6f));
        subtitleFont.draw(batch, "Created by Olivier, Luuk, Russell & Tim",
                0, 30, screenWidth, Align.center, false);
        subtitleFont.getData().setScale(subtitleScale);

        batch.end();
    }

    /**
     * Draws a button with shadow effect and scaling animation.
     */
    private void drawButtonWithShadow(Rectangle button, boolean hovered, float scale) {
        float centerX = button.x + button.width / 2f;
        float centerY = button.y + button.height / 2f;
        float scaledWidth = button.width * scale;
        float scaledHeight = button.height * scale;
        float scaledX = centerX - scaledWidth / 2f;
        float scaledY = centerY - scaledHeight / 2f;

        // Fixed shadow offset for virtual viewport
        float shadowOffset = 5;

        // Draw shadow
        shapeRenderer.setColor(SHADOW_COLOR);
        shapeRenderer.rect(scaledX + shadowOffset, scaledY - shadowOffset, scaledWidth, scaledHeight);

        // Draw button
        Color color = hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        shapeRenderer.setColor(color);
        shapeRenderer.rect(scaledX, scaledY, scaledWidth, scaledHeight);
    }

    /**
     * Draws button border with glow effect on hover.
     */
    private void drawButtonBorder(Rectangle button, boolean hovered, float scale) {
        float centerX = button.x + button.width / 2f;
        float centerY = button.y + button.height / 2f;
        float scaledWidth = button.width * scale;
        float scaledHeight = button.height * scale;
        float scaledX = centerX - scaledWidth / 2f;
        float scaledY = centerY - scaledHeight / 2f;

        Color borderColor = hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER_COLOR;
        if (hovered) {
            float glow = 0.5f + MathUtils.sin(animationTime * 5f) * 0.3f;
            borderColor = new Color(BUTTON_BORDER_HOVER.r * glow,
                    BUTTON_BORDER_HOVER.g * glow,
                    BUTTON_BORDER_HOVER.b * glow, 1.0f);
        }

        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(scaledX, scaledY, scaledWidth, scaledHeight);
    }

    /**
     * Draws button text with proper alignment and scaling.
     */
    private void drawButtonText(String text, Rectangle button, float scale) {
        float centerX = button.x + button.width / 2f;
        float centerY = button.y + button.height / 2f;

        buttonFont.getData().setScale(buttonFontScale * scale);
        glyphLayout.setText(buttonFont, text);

        float textX = centerX - glyphLayout.width / 2f;
        float textY = centerY + glyphLayout.height / 2f;

        // Fixed shadow offset for virtual viewport
        float shadowOffset = 2;

        // Text shadow
        buttonFont.setColor(SHADOW_COLOR);
        buttonFont.draw(batch, text, textX + shadowOffset, textY - shadowOffset);

        // Text main
        buttonFont.setColor(Color.WHITE);
        buttonFont.draw(batch, text, textX, textY);
    }

    /**
     * Called when the Play button is clicked.
     * Starts the game.
     */
    private void onPlayClicked() {
        System.out.println("[MenuScreen] Play button clicked - Starting game...");

        // Create and add GameScreen
        GameApp.addScreen("Game", new GameScreen());

        // Switch to game screen
        GameApp.switchScreen("Game");
    }

    /**
     * Called when the Settings button is clicked.
     * Currently not implemented - placeholder for future settings menu.
     */
    private void onSettingsClicked() {
        System.out.println("[MenuScreen] Settings button clicked (not yet implemented)");
        // TODO: Implement settings menu
    }

    /**
     * Called when the Multiplayer button is clicked.
     * Currently not implemented - placeholder for future multiplayer features.
     */
    private void onMultiplayerClicked() {
        System.out.println("[MenuScreen] Multiplayer button clicked (not yet implemented)");
        // TODO: Implement multiplayer functionality
    }

    @Override
    public void hide() {
        // Called when this screen is no longer active
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (buttonFont != null) {
            buttonFont.dispose();
        }
        if (subtitleFont != null) {
            subtitleFont.dispose();
        }
    }
}

