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

    // Button dimensions and positions
    private static final int BUTTON_WIDTH = 400;
    private static final int BUTTON_HEIGHT = 70;
    private static final int BUTTON_SPACING = 25;
    private static final int BUTTON_BORDER_WIDTH = 3;

    // Buttons (using Rectangle for hit detection)
    private Rectangle playButton;
    private Rectangle settingsButton;
    private Rectangle multiplayerButton;

    // Animation and timing
    private float animationTime = 0f;
    private float titlePulseTime = 0f;
    private float buttonAnimationDelay = 0f;
    private boolean animationsComplete = false;

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

        // Create fonts with better scaling
        titleFont = new BitmapFont();
        titleFont.getData().setScale(5.0f);
        titleFont.setColor(TITLE_COLOR);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.2f);
        buttonFont.setColor(Color.WHITE);

        subtitleFont = new BitmapFont();
        subtitleFont.getData().setScale(1.3f);
        subtitleFont.setColor(SUBTITLE_COLOR);


        // Calculate button positions (centered on screen)
        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        int centerX = (screenWidth - BUTTON_WIDTH) / 2;
        int startY = screenHeight / 2 + 20;

        playButton = new Rectangle(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT);
        settingsButton = new Rectangle(centerX, startY - BUTTON_HEIGHT - BUTTON_SPACING,
                BUTTON_WIDTH, BUTTON_HEIGHT);
        multiplayerButton = new Rectangle(centerX, startY - 2 * (BUTTON_HEIGHT + BUTTON_SPACING),
                BUTTON_WIDTH, BUTTON_HEIGHT);

        System.out.println("[MenuScreen] Enhanced main menu initialized");
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

        // Get mouse position
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getBackBufferHeight() - Gdx.input.getY();

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

        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        // Draw title with pulse effect
        float titlePulse = 1.0f + MathUtils.sin(titlePulseTime * 2f) * 0.05f;
        titleFont.getData().setScale(5.0f * titlePulse);

        // Title shadow
        titleFont.setColor(SHADOW_COLOR);
        titleFont.draw(batch, "MAZE SAHUR", 6, screenHeight - 94, screenWidth, Align.center, false);

        // Title main
        titleFont.setColor(TITLE_COLOR);
        titleFont.draw(batch, "MAZE SAHUR", 0, screenHeight - 100, screenWidth, Align.center, false);

        // Subtitle
        subtitleFont.draw(batch, "A Horror Maze Experience",
                0, screenHeight - 170, screenWidth, Align.center, false);

        // Draw button text with scaling
        drawButtonText("PLAY", playButton, playScale);
        drawButtonText("SETTINGS", settingsButton, settingsScale);
        drawButtonText("MULTIPLAYER", multiplayerButton, multiplayerScale);

        // Draw footer text
        subtitleFont.getData().setScale(0.9f);
        subtitleFont.setColor(new Color(0.4f, 0.4f, 0.4f, 0.6f));
        subtitleFont.draw(batch, "Created by Olivier, Luuk, Russell & Tim",
                0, 30, screenWidth, Align.center, false);
        subtitleFont.getData().setScale(1.3f);

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

        // Draw shadow
        shapeRenderer.setColor(SHADOW_COLOR);
        shapeRenderer.rect(scaledX + 5, scaledY - 5, scaledWidth, scaledHeight);

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

        buttonFont.getData().setScale(2.2f * scale);
        glyphLayout.setText(buttonFont, text);

        float textX = centerX - glyphLayout.width / 2f;
        float textY = centerY + glyphLayout.height / 2f;

        // Text shadow
        buttonFont.setColor(SHADOW_COLOR);
        buttonFont.draw(batch, text, textX + 2, textY - 2);

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

