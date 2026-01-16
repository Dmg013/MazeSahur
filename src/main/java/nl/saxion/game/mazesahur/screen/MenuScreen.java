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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import nl.saxion.game.mazesahur.model.CharacterType;
import nl.saxion.game.mazesahur.model.CurrencyManager;
import nl.saxion.game.mazesahur.net.MultiplayerSession;
import nl.saxion.game.mazesahur.net.NetworkDefaults;
import nl.saxion.game.mazesahur.net.NetworkSessionConfig;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import nl.saxion.game.mazesahur.screen.LoadingScreen;

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

    // Textures for menu
    private Texture backgroundTexture;
    private Texture singleplayerTexture;
    private Texture multiplayerTexture;
    private Texture lootcratesTexture;

    // Button dimensions and positions (as percentages of screen size)
    private static final float BUTTON_WIDTH_PERCENT = 0.31f;  // 31% of screen width
    private static final float BUTTON_HEIGHT_PERCENT = 0.097f; // ~9.7% of screen height
    private static final float BUTTON_SPACING_PERCENT = 0.035f; // 3.5% of screen height
    private static final int BUTTON_BORDER_WIDTH = 3;

    // Scale factor for button textures (to make them smaller)
    private static final float BUTTON_TEXTURE_SCALE = 0.3f;

    // Actual button dimensions (calculated based on screen size)
    private int buttonWidth;
    private int buttonHeight;
    private int buttonSpacing;

    // Buttons (using Rectangle for hit detection)
    private Rectangle playButton;
    private Rectangle lootCratesButton;
    private Rectangle multiplayerButton;

    // Currency manager
    private CurrencyManager currencyManager;

    // Animation and timing
    private float animationTime = 0f;
    private float titlePulseTime = 0f;
    private float buttonAnimationDelay = 0f;
    private boolean animationsComplete = false;

    // Font scales (calculated based on screen size)
    private float titleScale;
    private float buttonFontScale;
    private float subtitleScale;

    // Multiplayer form
    private boolean showMultiplayerForm = false;
    private String serverField;
    private String roomField;
    private String nameField;
    private FormField activeField = FormField.SERVER;
    private final InputAdapter formInputProcessor = new InputAdapter() {
        @Override
        public boolean keyTyped(final char character) {
            if (!showMultiplayerForm || activeField == null) {
                return false;
            }
            if (character == '\b') { // backspace
                backspaceActiveField();
                return true;
            }
            if (character == '\r' || character == '\n') { // enter
                startMultiplayer(serverField, roomField, nameField);
                return true;
            }
            if (character >= 32 && character < 127) {
                appendToActiveField(character);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(final int keycode) {
            if (!showMultiplayerForm) {
                return false;
            }
            if (keycode == Input.Keys.ESCAPE) {
                showMultiplayerForm = false;
                activeField = FormField.SERVER;
                Gdx.input.setInputProcessor(null);
                return true;
            }
            if (keycode == Input.Keys.TAB) {
                cycleField();
                return true;
            }
            return false;
        }
    };

    private enum FormField {
        SERVER,
        ROOM,
        NAME
    }

    // Virtual viewport dimensions
    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;
    private final Matrix4 uiProjection = new Matrix4();
    private final Vector2 mouseBuffer = new Vector2();
    private float viewportScale = 1f;
    private float viewportWidth = VIEWPORT_WIDTH;
    private float viewportHeight = VIEWPORT_HEIGHT;
    private float viewportX = 0f;
    private float viewportY = 0f;

    // Colors - Modern dark theme with red accents
    private static final Color BUTTON_COLOR = new Color(0.12f, 0.12f, 0.15f, 0.95f);
    private static final Color BUTTON_HOVER_COLOR = new Color(0.5f, 0.05f, 0.05f, 0.95f);
    private static final Color BUTTON_BORDER_COLOR = new Color(0.7f, 0.1f, 0.1f, 1.0f);
    private static final Color BUTTON_BORDER_HOVER = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color SUBTITLE_COLOR = new Color(0.6f, 0.6f, 0.6f, 0.8f);
    private static final Color SHADOW_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.6f);
    private static final Color PANEL_COLOR = new Color(0.08f, 0.08f, 0.1f, 0.92f);
    private static final Color FIELD_BG = new Color(0.15f, 0.15f, 0.18f, 0.95f);
    private static final Color FIELD_BG_ACTIVE = new Color(0.2f, 0.2f, 0.25f, 0.95f);
    private static final Color FIELD_BORDER = new Color(0.7f, 0.1f, 0.1f, 1.0f);

    /**
     * Creates a new menu screen.
     */
    public MenuScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        if (!Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
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

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("img/menu_scherm.png"));
        singleplayerTexture = new Texture(Gdx.files.internal("img/Singleplayer.png"));
        multiplayerTexture = new Texture(Gdx.files.internal("img/multiplayer.png"));
        lootcratesTexture = new Texture(Gdx.files.internal("img/Lootcrates.png"));

        // Initialize currency manager
        currencyManager = new CurrencyManager();

        // Initialize responsive layout
        calculateResponsiveLayout();
        updateViewportTransform();

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
        // Use texture dimensions if available, otherwise use calculated dimensions
        if (playButton == null) {
            playButton = new Rectangle();
            lootCratesButton = new Rectangle();
            multiplayerButton = new Rectangle();
        }

        // Set button rectangles based on texture dimensions for proper hit detection
        int centerX = screenWidth / 2;
        int startY = screenHeight / 2 - 20;  // Moved down significantly

        if (singleplayerTexture != null) {
            float btnWidth = singleplayerTexture.getWidth() * BUTTON_TEXTURE_SCALE;
            float btnHeight = singleplayerTexture.getHeight() * BUTTON_TEXTURE_SCALE;
            float spacing = -112;  // Half of previous spacing, buttons very tightly packed
            playButton.set(centerX - btnWidth / 2, startY, btnWidth, btnHeight);
            multiplayerButton.set(centerX - btnWidth / 2, startY - btnHeight - spacing, btnWidth, btnHeight);
            lootCratesButton.set(centerX - btnWidth / 2, startY - 2 * (btnHeight + spacing), btnWidth, btnHeight);
        } else {
            playButton.set(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight);
            multiplayerButton.set(centerX - buttonWidth / 2, startY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight);
            lootCratesButton.set(centerX - buttonWidth / 2, startY - 2 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        updateViewportTransform();
    }

    @Override
    public void render(final float delta) {
        super.render(delta);
        updateViewportTransform();

        // Update animations
        animationTime += delta;
        titlePulseTime += delta;

        if (buttonAnimationDelay < 0.5f) {
            buttonAnimationDelay += delta;
        } else if (!animationsComplete) {
            animationsComplete = true;
        }

        // Clear screen with black background
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);
        batch.setProjectionMatrix(uiProjection);
        shapeRenderer.setProjectionMatrix(uiProjection);

        // Get mouse position (convert from screen to viewport coordinates)
        final Vector2 mousePos = getMouseInViewport();
        final int mouseX = MathUtils.floor(mousePos.x);
        final int mouseY = MathUtils.floor(mousePos.y);

        // Check hover states
        boolean playHovered = !showMultiplayerForm && playButton.contains(mouseX, mouseY);
        boolean lootCratesHovered = !showMultiplayerForm && lootCratesButton.contains(mouseX, mouseY);
        boolean multiplayerHovered = !showMultiplayerForm && multiplayerButton.contains(mouseX, mouseY);

        // Handle clicks
        if (!showMultiplayerForm && Gdx.input.justTouched()) {
            if (playHovered) {
                onPlayClicked();
            } else if (lootCratesHovered) {
                onLootCratesClicked();
            } else if (multiplayerHovered) {
                onMultiplayerClicked();
            }
        }

        // Enable blending for transparency effects
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Calculate hover scales for animations
        float playScale = animationsComplete ? (playHovered ? 1.15f : 1.0f) :
                          Interpolation.elasticOut.apply(0f, 1.0f, Math.min(1f, animationTime * 2f));
        float lootCratesScale = animationsComplete ? (lootCratesHovered ? 1.15f : 1.0f) :
                                Interpolation.elasticOut.apply(0f, 1.0f, Math.min(1f, (animationTime - 0.1f) * 2f));
        float multiplayerScale = animationsComplete ? (multiplayerHovered ? 1.15f : 1.0f) :
                                 Interpolation.elasticOut.apply(0f, 1.0f, Math.min(1f, (animationTime - 0.15f) * 2f));

        // Start drawing with SpriteBatch
        batch.begin();

        // Draw background image
        batch.draw(backgroundTexture, 0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        // Draw button PNG images with hover scaling
        drawButtonTexture(singleplayerTexture, playButton, playScale);
        drawButtonTexture(lootcratesTexture, lootCratesButton, lootCratesScale);
        drawButtonTexture(multiplayerTexture, multiplayerButton, multiplayerScale);

        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Render multiplayer overlay last
        if (showMultiplayerForm) {
            renderMultiplayerForm(mouseX, mouseY);
        }

        // Reset line width
        Gdx.gl.glLineWidth(1f);
    }

    /**
     * Draws a button texture with scaling animation centered on the button position.
     * Preserves the original aspect ratio of the texture.
     */
    private void drawButtonTexture(Texture texture, Rectangle button, float scale) {
        float centerX = button.x + button.width / 2f;
        float centerY = button.y + button.height / 2f;

        // Use texture's original dimensions with base scale and hover scale
        float originalWidth = texture.getWidth() * BUTTON_TEXTURE_SCALE;
        float originalHeight = texture.getHeight() * BUTTON_TEXTURE_SCALE;

        float scaledWidth = originalWidth * scale;
        float scaledHeight = originalHeight * scale;

        float scaledX = centerX - scaledWidth / 2f;
        float scaledY = centerY - scaledHeight / 2f;

        batch.draw(texture, scaledX, scaledY, scaledWidth, scaledHeight);
    }

    private void updateViewportTransform() {
        final float windowWidth = Gdx.graphics.getWidth();
        final float windowHeight = Gdx.graphics.getHeight();
        final float scaleX = windowWidth / VIEWPORT_WIDTH;
        final float scaleY = windowHeight / VIEWPORT_HEIGHT;
        viewportScale = Math.min(scaleX, scaleY);
        viewportWidth = VIEWPORT_WIDTH * viewportScale;
        viewportHeight = VIEWPORT_HEIGHT * viewportScale;
        viewportX = (windowWidth - viewportWidth) / 2f;
        viewportY = (windowHeight - viewportHeight) / 2f;

        uiProjection.setToOrtho2D(0f, 0f, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        if (batch != null) {
            batch.setProjectionMatrix(uiProjection);
        }
        if (shapeRenderer != null) {
            shapeRenderer.setProjectionMatrix(uiProjection);
        }
    }

    private Vector2 getMouseInViewport() {
        final float screenX = Gdx.input.getX();
        final float screenY = Gdx.input.getY();
        final float virtualX = (screenX - viewportX) / viewportScale;
        final float virtualY = (Gdx.graphics.getHeight() - screenY - viewportY) / viewportScale;
        return mouseBuffer.set(virtualX, virtualY);
    }


    private void openCharacterSelection(final String screenName, final Consumer<CharacterType> onSelected) {
        final CharacterSelectionScreen selectionScreen = new CharacterSelectionScreen(characterType -> {
            if (onSelected != null) {
                onSelected.accept(characterType);
            }
        });
        GameApp.addScreen(screenName, selectionScreen);
        GameApp.switchScreen(screenName);
    }

    /**
     * Called when the Play button is clicked.
     * Starts the game.
     */
    private void onPlayClicked() {
        System.out.println("[MenuScreen] Play button clicked - Starting game...");

        openCharacterSelection("CharacterSelectSingleplayer", selectedCharacter -> {
            final LoadingScreen loadingScreen = new LoadingScreen(
                "Loading world...",
                () -> {
                    final GameScreen gameScreen = new GameScreen(null, null, selectedCharacter);
                    GameApp.addScreen("Game", gameScreen);
                },
                () -> {
                    GameApp.switchScreen("Game");
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                }
            );
            GameApp.addScreen("LoadingGame", loadingScreen);
            GameApp.switchScreen("LoadingGame");
        });
    }

    /**
     * Called when the Loot Crates button is clicked.
     */
    private void onLootCratesClicked() {
        System.out.println("[MenuScreen] Loot Crates button clicked");
        LootCratesScreen lootScreen = new LootCratesScreen();
        GameApp.addScreen("LootCrates", lootScreen);
        GameApp.switchScreen("LootCrates");
    }

    /**
     * Called when the Multiplayer button is clicked.
     * Currently not implemented - placeholder for future multiplayer features.
     */
    private void onMultiplayerClicked() {
        System.out.println("[MenuScreen] Multiplayer button clicked - Showing form");
        serverField = NetworkDefaults.serverUrl();
        roomField = NetworkDefaults.room();
        nameField = NetworkDefaults.playerName();
        activeField = FormField.SERVER;
        showMultiplayerForm = true;
        Gdx.input.setInputProcessor(formInputProcessor);
    }

    private void startMultiplayer(final String serverUrl, final String room, final String playerName) {
        showMultiplayerForm = false;
        Gdx.input.setInputProcessor(null);

        openCharacterSelection("CharacterSelectMultiplayer", selectedCharacter -> {
            final NetworkSessionConfig config = new NetworkSessionConfig(serverUrl, room, playerName, selectedCharacter);

            final MultiplayerSession session = new MultiplayerSession(config);
            final boolean joined = session.connectAndAwaitJoin(java.time.Duration.ofSeconds(5));
            if (!joined) {
                System.out.println("[MenuScreen] Multiplayer join failed");
                GameApp.switchScreen("Menu");
                return;
            }

            final LoadingScreen loadingScreen = new LoadingScreen(
                "Loading world...",
                () -> {
                    final GameScreen gameScreen = new GameScreen(session.getSeed(), session, selectedCharacter);
                    GameApp.addScreen("Game", gameScreen);
                },
                () -> {
                    GameApp.switchScreen("Game");
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                }
            );
            GameApp.addScreen("LoadingGameMultiplayer", loadingScreen);
            GameApp.switchScreen("LoadingGameMultiplayer");
        });
    }

    private void renderMultiplayerForm(final int mouseX, final int mouseY) {
        final boolean clicked = Gdx.input.justTouched();

        // Dim background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.6f));
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        final float panelWidth = 760f;
        final float panelHeight = 380f;
        final float panelX = (VIEWPORT_WIDTH - panelWidth) / 2f;
        final float panelY = (VIEWPORT_HEIGHT - panelHeight) / 2f;

        final Rectangle serverRect = new Rectangle(panelX + 40, panelY + panelHeight - 120, panelWidth - 80, 50);
        final Rectangle roomRect = new Rectangle(panelX + 40, panelY + panelHeight - 190, panelWidth - 80, 50);
        final Rectangle nameRect = new Rectangle(panelX + 40, panelY + panelHeight - 260, panelWidth - 80, 50);
        final Rectangle connectRect = new Rectangle(panelX + panelWidth - 200, panelY + 30, 160, 50);

        shapeRenderer.setColor(PANEL_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        renderFieldRect(serverRect, activeField == FormField.SERVER);
        renderFieldRect(roomRect, activeField == FormField.ROOM);
        renderFieldRect(nameRect, activeField == FormField.NAME);

        shapeRenderer.setColor(BUTTON_COLOR);
        shapeRenderer.rect(connectRect.x, connectRect.y, connectRect.width, connectRect.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(FIELD_BORDER);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.rect(serverRect.x, serverRect.y, serverRect.width, serverRect.height);
        shapeRenderer.rect(roomRect.x, roomRect.y, roomRect.width, roomRect.height);
        shapeRenderer.rect(nameRect.x, nameRect.y, nameRect.width, nameRect.height);
        shapeRenderer.setColor(BUTTON_BORDER_COLOR);
        shapeRenderer.rect(connectRect.x, connectRect.y, connectRect.width, connectRect.height);
        shapeRenderer.end();

        batch.begin();
        subtitleFont.getData().setScale(0.9f);
        subtitleFont.setColor(Color.LIGHT_GRAY);
        subtitleFont.draw(batch, "Multiplayer", panelX, panelY + panelHeight - 40, panelWidth, Align.center, false);

        drawFieldText("Server URL", serverField, serverRect);
        drawFieldText("Room", roomField, roomRect);
        drawFieldText("Naam", nameField, nameRect);

        buttonFont.getData().setScale(1.0f);
        glyphLayout.setText(buttonFont, "CONNECT");
        buttonFont.setColor(Color.WHITE);
        buttonFont.draw(batch, "CONNECT",
            connectRect.x + (connectRect.width - glyphLayout.width) / 2f,
            connectRect.y + (connectRect.height + glyphLayout.height) / 2f);
        batch.end();

        if (clicked) {
            if (serverRect.contains(mouseX, mouseY)) {
                activeField = FormField.SERVER;
            } else if (roomRect.contains(mouseX, mouseY)) {
                activeField = FormField.ROOM;
            } else if (nameRect.contains(mouseX, mouseY)) {
                activeField = FormField.NAME;
            } else if (connectRect.contains(mouseX, mouseY)) {
                startMultiplayer(serverField, roomField, nameField);
            }
        }
    }

    private void renderFieldRect(final Rectangle rect, final boolean active) {
        shapeRenderer.setColor(active ? FIELD_BG_ACTIVE : FIELD_BG);
        shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
    }

    private void drawFieldText(final String label, final String value, final Rectangle rect) {
        subtitleFont.getData().setScale(0.7f);
        subtitleFont.setColor(SUBTITLE_COLOR);
        subtitleFont.draw(batch, label, rect.x + 6, rect.y + rect.height - 8);

        buttonFont.getData().setScale(0.8f);
        buttonFont.setColor(Color.WHITE);
        buttonFont.draw(batch, value, rect.x + 10, rect.y + rect.height / 2f + 10);
    }

    private void appendToActiveField(final char character) {
        switch (activeField) {
            case SERVER:
                serverField = appendClamped(serverField, character);
                break;
            case ROOM:
                roomField = appendClamped(roomField, character);
                break;
            case NAME:
                nameField = appendClamped(nameField, character);
                break;
            default:
                break;
        }
    }

    private String appendClamped(final String original, final char c) {
        if (original.length() >= 120) {
            return original;
        }
        return original + c;
    }

    private void backspaceActiveField() {
        switch (activeField) {
            case SERVER:
                serverField = backspace(serverField);
                break;
            case ROOM:
                roomField = backspace(roomField);
                break;
            case NAME:
                nameField = backspace(nameField);
                break;
            default:
                break;
        }
    }

    private String backspace(final String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, value.length() - 1);
    }

    private void cycleField() {
        switch (activeField) {
            case SERVER:
                activeField = FormField.ROOM;
                break;
            case ROOM:
                activeField = FormField.NAME;
                break;
            case NAME:
            default:
                activeField = FormField.SERVER;
                break;
        }
    }

    @Override
    public void hide() {
        // Called when this screen is no longer active
        Gdx.input.setInputProcessor(null);
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
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (singleplayerTexture != null) {
            singleplayerTexture.dispose();
        }
        if (multiplayerTexture != null) {
            multiplayerTexture.dispose();
        }
        if (lootcratesTexture != null) {
            lootcratesTexture.dispose();
        }
    }
}
