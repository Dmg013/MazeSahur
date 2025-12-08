package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import java.util.HashMap;
import java.util.Map;
import nl.saxion.game.mazesahur.model.CharacterType;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.gameapp.GameApp;
import java.util.function.Consumer;

/**
 * Modern character selection screen where players choose their skin/model.
 * Matches the menu screen's dark theme and styling.
 *
 * @author Tim
 * @version 1.0
 */
public class CharacterSelectionScreen extends ScalableGameScreen {
    private static final String FONT_NAME = "ui";
    private static final int FONT_SIZE = 20;

    // Virtual viewport dimensions (same as MenuScreen)
    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;

    // Colors - Modern dark theme matching menu
    private static final Color PANEL_BG = new Color(0.08f, 0.08f, 0.1f, 0.92f);
    private static final Color BUTTON_COLOR = new Color(0.12f, 0.12f, 0.15f, 0.95f);
    private static final Color BUTTON_HOVER_COLOR = new Color(0.5f, 0.05f, 0.05f, 0.95f);
    private static final Color BUTTON_BORDER_COLOR = new Color(0.7f, 0.1f, 0.1f, 1.0f);
    private static final Color BUTTON_BORDER_HOVER = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color TEXT_DIM = new Color(0.6f, 0.6f, 0.6f, 0.8f);

    private int selectedIndex = 0;
    private final CharacterType[] characters = CharacterType.values();
    private boolean keyPressed = false;
    private CharacterType lastPreviewType = null;
    private float previewRotation = 0f;

    // UI rendering
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;
    private GlyphLayout layout;
    private Rectangle[] characterButtons;
    private Rectangle selectButton;
    private final Matrix4 uiProjection = new Matrix4();
    private final Vector2 mouseBuffer = new Vector2();
    private float viewportScale = 1f;
    private float viewportWidth = VIEWPORT_WIDTH;
    private float viewportHeight = VIEWPORT_HEIGHT;
    private float viewportX = 0f;
    private float viewportY = 0f;

    // Callback for when character is selected
    private final Consumer<CharacterType> onCharacterSelected;

    // Preview rendering resources
    private ModelBatch previewBatch;
    private Environment previewEnvironment;
    private PerspectiveCamera previewCamera;
    private final Map<CharacterType, Model> previewModels = new HashMap<>();
    private final Map<CharacterType, ModelInstance> previewInstances = new HashMap<>();
    private final Map<CharacterType, AnimationController> previewControllers = new HashMap<>();
    private final Map<CharacterType, Float> previewScales = new HashMap<>();
    private final Map<CharacterType, Float> previewFootOffsets = new HashMap<>();
    private Texture previewFallbackTexture;

    public CharacterSelectionScreen(final Consumer<CharacterType> onCharacterSelected) {
        super(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        this.onCharacterSelected = onCharacterSelected;
    }

    @Override
    public void show() {
        System.out.println("[CharacterSelectionScreen] Showing modern character selection");

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        // Create fonts with modern styling
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.5f);
        titleFont.setColor(TITLE_COLOR);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.8f);
        buttonFont.setColor(TEXT_COLOR);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.2f);
        smallFont.setColor(TEXT_DIM);

        initPreviewRenderer();
        initializeButtons();
        updateViewportTransform();

        // Load saved character selection
        final String savedCharacter = Gdx.app.getPreferences("MazeSahur").getString("selectedCharacter", "DEFAULT");
        for (int i = 0; i < characters.length; i++) {
            if (characters[i].name().equals(savedCharacter)) {
                selectedIndex = i;
                break;
            }
        }
    }

    private void initializeButtons() {
        // Use virtual viewport dimensions for consistent layout
        final int screenWidth = VIEWPORT_WIDTH;
        final int screenHeight = VIEWPORT_HEIGHT;

        characterButtons = new Rectangle[characters.length];

        // Right panel takes up right half of screen
        final int buttonWidth = 350;
        final int buttonHeight = 70;
        final int buttonSpacing = 15;
        // Center buttons in the right half of the screen
        final int rightPanelStart = screenWidth / 2;
        final int rightPanelX = rightPanelStart + (screenWidth / 2 - buttonWidth) / 2;

        int startY = screenHeight / 2 + (characters.length * (buttonHeight + buttonSpacing)) / 2;

        for (int i = 0; i < characters.length; i++) {
            characterButtons[i] = new Rectangle(
                rightPanelX,
                startY - i * (buttonHeight + buttonSpacing),
                buttonWidth,
                buttonHeight
            );
        }

        // Select button at bottom, centered in right half
        selectButton = new Rectangle(
            rightPanelX,
            100,
            buttonWidth,
            buttonHeight
        );
    }

    @Override
    public void render(float delta) {
        // Clear screen with dark background
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Call parent to set up viewport/camera
        super.render(delta);

        updateViewportTransform();

        // Handle input
        handleInput();

        // Render modern UI first (while viewport is correctly set)
        renderModernUI();

        // Render 3D preview on left side (changes viewport temporarily)
        renderPreview(delta);
    }

    private void handleInput() {
        // Convert screen coordinates to the virtual viewport (handles letterboxing)
        final Vector2 mousePos = getMouseInViewport();
        final float mouseX = mousePos.x;
        final float mouseY = mousePos.y;

        // Check character button hover/click
        for (int i = 0; i < characterButtons.length; i++) {
            if (characterButtons[i].contains(mouseX, mouseY)) {
                selectedIndex = i;
                if (Gdx.input.justTouched()) {
                    selectCharacter();
                }
            }
        }

        // Check select button
        if (selectButton.contains(mouseX, mouseY) && Gdx.input.justTouched()) {
            selectCharacter();
        }

        // Keyboard input (prevent key repeat)
        final boolean upPressed = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        final boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
        final boolean enterPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (!keyPressed) {
            if (upPressed) {
                selectedIndex = (selectedIndex - 1 + characters.length) % characters.length;
                keyPressed = true;
            } else if (downPressed) {
                selectedIndex = (selectedIndex + 1) % characters.length;
                keyPressed = true;
            } else if (enterPressed) {
                selectCharacter();
                keyPressed = true;
            }
        }

        if (!upPressed && !downPressed && !enterPressed) {
            keyPressed = false;
        }
    }

    private void renderModernUI() {
        final int screenWidth = VIEWPORT_WIDTH;
        final int screenHeight = VIEWPORT_HEIGHT;

        // Convert screen to virtual viewport coordinates
        final Vector2 mousePos = getMouseInViewport();
        final float mouseX = mousePos.x;
        final float mouseY = mousePos.y;

        // Ensure batch and shapes use the virtual viewport projection
        batch.setProjectionMatrix(uiProjection);
        shapeRenderer.setProjectionMatrix(uiProjection);

        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // GameApp uses drawText which sets up the camera correctly
        // We'll piggyback on that by using GameApp's drawText for positioning

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw character selection buttons
        for (int i = 0; i < characterButtons.length; i++) {
            final Rectangle button = characterButtons[i];
            final boolean isHovered = button.contains(mouseX, mouseY);
            final boolean isSelected = i == selectedIndex;

            Color bgColor = BUTTON_COLOR;
            if (isSelected) {
                bgColor = BUTTON_HOVER_COLOR;
            } else if (isHovered) {
                bgColor = new Color(0.18f, 0.18f, 0.2f, 0.95f);
            }

            shapeRenderer.setColor(bgColor);
            shapeRenderer.rect(button.x, button.y, button.width, button.height);
        }

        // Draw select button
        final boolean selectHovered = selectButton.contains(mouseX, mouseY);
        shapeRenderer.setColor(selectHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(selectButton.x, selectButton.y, selectButton.width, selectButton.height);

        shapeRenderer.end();

        // Draw button borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);

        for (int i = 0; i < characterButtons.length; i++) {
            final Rectangle button = characterButtons[i];
            final boolean isHovered = button.contains(mouseX, mouseY);
            final boolean isSelected = i == selectedIndex;

            Color borderColor = BUTTON_BORDER_COLOR;
            if (isSelected || isHovered) {
                borderColor = BUTTON_BORDER_HOVER;
            }

            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(button.x, button.y, button.width, button.height);
        }

        // Select button border
        shapeRenderer.setColor(selectHovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER_COLOR);
        shapeRenderer.rect(selectButton.x, selectButton.y, selectButton.width, selectButton.height);

        shapeRenderer.end();

        // Draw text using batch with same projection as shapes
        batch.begin();

        // Title at top
        titleFont.setColor(TITLE_COLOR);
        layout.setText(titleFont, "SELECT CHARACTER");
        titleFont.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight - 50);

        // Character names on buttons
        for (int i = 0; i < characterButtons.length; i++) {
            final Rectangle button = characterButtons[i];
            final CharacterType character = characters[i];
            final boolean isSelected = i == selectedIndex;

            buttonFont.setColor(isSelected ? Color.WHITE : TEXT_COLOR);
            layout.setText(buttonFont, character.getDisplayName());
            buttonFont.draw(batch, layout,
                button.x + (button.width - layout.width) / 2f,
                button.y + (button.height + layout.height) / 2f);
        }

        // Select button text
        buttonFont.setColor(Color.WHITE);
        layout.setText(buttonFont, "CONFIRM SELECTION");
        buttonFont.draw(batch, layout,
            selectButton.x + (selectButton.width - layout.width) / 2f,
            selectButton.y + (selectButton.height + layout.height) / 2f);

        // Instructions at bottom
        smallFont.setColor(TEXT_DIM);
        final String instructions = "Arrow Keys or W/S to navigate  •  ENTER or Click to select";
        layout.setText(smallFont, instructions);
        smallFont.draw(batch, layout, (screenWidth - layout.width) / 2f, 40);

        // Preview label on left side
        titleFont.getData().setScale(2.0f);
        titleFont.setColor(TITLE_COLOR);
        final String previewLabel = "PREVIEW";
        layout.setText(titleFont, previewLabel);
        titleFont.draw(batch, layout, (screenWidth / 2f - layout.width) / 2f, screenHeight - 50);
        titleFont.getData().setScale(3.5f); // Reset scale

        batch.end();
    }

    private void initPreviewRenderer() {
        previewBatch = new ModelBatch();
        previewEnvironment = new Environment();
        previewEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        previewEnvironment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        // Use virtual viewport dimensions for camera (left half)
        previewCamera = new PerspectiveCamera(50f, VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT);
        previewCamera.position.set(0f, 1.3f, 3.2f);
        previewCamera.lookAt(0f, 1.0f, 0f);
        previewCamera.near = 0.05f;
        previewCamera.far = 20f;
        previewCamera.update();
    }

    private void renderPreview(final float delta) {
        if (previewBatch == null || previewCamera == null) {
            return;
        }

        final CharacterType selected = characters[selectedIndex];

        // Set viewport for left half of virtual viewport
        final int previewWidth = (int) (viewportWidth / 2f);
        final int previewHeight = (int) viewportHeight;
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, previewWidth, previewHeight);

        previewCamera.viewportWidth = VIEWPORT_WIDTH / 2f;
        previewCamera.viewportHeight = VIEWPORT_HEIGHT;
        previewCamera.update();

        // Load model if needed
        ensurePreviewModel(selected);

        final ModelInstance instance = previewInstances.get(selected);
        final AnimationController controller = previewControllers.get(selected);
        final float modelScale = previewScales.getOrDefault(selected, 1f);
        final float footOffset = previewFootOffsets.getOrDefault(selected, 0f);

        // Rotate and position
        previewRotation += delta * 30f;
        instance.transform.idt();
        instance.transform.translate(0f, footOffset * modelScale, 0f);
        instance.transform.rotate(Vector3.Y, previewRotation);
        instance.transform.scale(modelScale, modelScale, modelScale);
        instance.calculateTransforms();

        if (controller != null) {
            controller.update(delta);
        }

        // Render model
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        previewBatch.begin(previewCamera);
        previewBatch.render(instance, previewEnvironment);
        previewBatch.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        // Restore full viewport for UI rendering
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);
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

    private void ensurePreviewModel(final CharacterType type) {
        if (previewModels.containsKey(type)) {
            if (lastPreviewType != type) {
                final AnimationController controller = previewControllers.get(type);
                if (controller != null) {
                    controller.setAnimation("walking", -1);
                }
            }
            lastPreviewType = type;
            return;
        }

        final String basePath = type.getModelPath();
        final String walkingFile = type == CharacterType.DEFAULT ? "WalkingNew" : "Walking";
        final String modelPath = basePath + "/" + walkingFile + ".g3dj";

        final G3dModelLoader loader = new G3dModelLoader(new JsonReader());
        final FileHandle modelFile = Gdx.files.internal(modelPath);
        final Model model = loader.loadModel(modelFile, createTextureProvider(modelFile));
        applyMaterialTextures(model, modelFile.parent());

        if (model.animations.size > 0) {
            final Animation walkingAnim = model.animations.first();
            walkingAnim.id = "walking";
        }

        final ModelInstance tempInstance = new ModelInstance(model);
        final BoundingBox bounds = new BoundingBox();
        tempInstance.calculateBoundingBox(bounds);
        final Vector3 dimensions = bounds.getDimensions(new Vector3());
        float scale = 1f;
        float footOffset = -bounds.min.y;
        if (dimensions.y > 0.0001f) {
            scale = (GameConfig.PLAYER_HEIGHT / dimensions.y) * 1.1f;
        }

        final ModelInstance instance = new ModelInstance(model);
        AnimationController controller = null;
        if (model.animations.size > 0) {
            controller = new AnimationController(instance);
            controller.setAnimation("walking", -1);
        }

        previewModels.put(type, model);
        previewInstances.put(type, instance);
        if (controller != null) {
            previewControllers.put(type, controller);
        }
        previewScales.put(type, scale);
        previewFootOffsets.put(type, footOffset);
        lastPreviewType = type;
    }

    private void applyMaterialTextures(final Model model, final FileHandle baseDir) {
        if (model == null || baseDir == null) {
            return;
        }
        for (Material mat : model.materials) {
            final String name = mat.id != null ? mat.id.toLowerCase() : "";
            final boolean isHair = name.contains("hair");

            final FileHandle diffuseFile = pickTexture(baseDir, isHair, "diffuse");
            if (diffuseFile != null) {
                final Texture diffuseTex = new Texture(diffuseFile, true);
                diffuseTex.setFilter(
                    Texture.TextureFilter.MipMapLinearLinear,
                    Texture.TextureFilter.Linear
                );
                diffuseTex.setWrap(
                    Texture.TextureWrap.Repeat,
                    Texture.TextureWrap.Repeat
                );
                mat.set(TextureAttribute.createDiffuse(diffuseTex));
            }

            final FileHandle normalFile = pickTexture(baseDir, isHair, "normal");
            if (normalFile != null) {
                final Texture normalTex = new Texture(normalFile, true);
                normalTex.setFilter(
                    Texture.TextureFilter.MipMapLinearLinear,
                    Texture.TextureFilter.Linear
                );
                normalTex.setWrap(
                    Texture.TextureWrap.Repeat,
                    Texture.TextureWrap.Repeat
                );
                mat.set(TextureAttribute.createNormal(normalTex));
            }

            final FileHandle specFile = pickTexture(baseDir, isHair, "specular");
            if (specFile != null) {
                final Texture specTex = new Texture(specFile, true);
                specTex.setFilter(
                    Texture.TextureFilter.MipMapLinearLinear,
                    Texture.TextureFilter.Linear
                );
                specTex.setWrap(
                    Texture.TextureWrap.Repeat,
                    Texture.TextureWrap.Repeat
                );
                mat.set(TextureAttribute.createSpecular(specTex));
            }
        }
    }

    private FileHandle pickTexture(final FileHandle baseDir, final boolean hair, final String key) {
        final String suffix = key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();

        // Try both Ch33 (Big Business) and Ch06 (Soundcloud) naming patterns
        final String[] primaries = hair
            ? new String[]{"Ch33_1002_" + suffix + ".png", "Ch06_1002_" + suffix + ".png"}
            : new String[]{"Ch33_1001_" + suffix + ".png", "Ch06_1001_" + suffix + ".png"};

        for (String primary : primaries) {
            FileHandle handle = baseDir.child(primary);
            if (handle.exists()) {
                return handle;
            }
        }

        // Fallback: search for any file containing the key
        for (FileHandle fh : baseDir.list()) {
            if (fh.name().toLowerCase().contains(key.toLowerCase())) {
                return fh;
            }
        }
        return null;
    }

    private TextureProvider createTextureProvider(final FileHandle modelFile) {
        final FileHandle baseDir = modelFile.parent();
        return fileName -> {
            try {
                FileHandle handle = baseDir.child(fileName);
                if (!handle.exists()) {
                    handle = Gdx.files.internal(fileName);
                }
                return new Texture(handle);
            } catch (Exception e) {
                if (previewFallbackTexture == null) {
                    previewFallbackTexture = buildFallbackTexture();
                }
                return previewFallbackTexture;
            }
        };
    }

    private Texture buildFallbackTexture() {
        final Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        final Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void selectCharacter() {
        final CharacterType selected = characters[selectedIndex];

        // Save selection to preferences
        Gdx.app.getPreferences("MazeSahur").putString("selectedCharacter", selected.name());
        Gdx.app.getPreferences("MazeSahur").flush();

        System.out.println("[CharacterSelectionScreen] Selected character: " + selected.name());

        // Call callback to proceed
        if (onCharacterSelected != null) {
            onCharacterSelected.accept(selected);
        }
    }

    public CharacterType getSelectedCharacter() {
        return characters[selectedIndex];
    }

    public static CharacterType getSavedCharacter() {
        final String savedName = Gdx.app.getPreferences("MazeSahur").getString("selectedCharacter", "DEFAULT");
        return CharacterType.fromString(savedName);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // Buttons use virtual coordinates, so no need to reinitialize
    }

    @Override
    public void hide() {
        // Clean up resources when hiding
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
        if (smallFont != null) {
            smallFont.dispose();
        }
        if (previewBatch != null) {
            previewBatch.dispose();
        }
        for (Model model : previewModels.values()) {
            model.dispose();
        }
        previewModels.clear();
        if (previewFallbackTexture != null) {
            previewFallbackTexture.dispose();
            previewFallbackTexture = null;
        }
    }
}
