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
import nl.saxion.game.mazesahur.model.UnlockManager;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.gameapp.GameApp;
import java.util.function.Consumer;
import nl.saxion.game.mazesahur.rendering.ResourceManager;

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
    // Only characters with button textures (BIG_BUSINESS has no texture)
    private final CharacterType[] characters = {
        CharacterType.DEFAULT,
        CharacterType.SOUNDCLOUD,
        CharacterType.LOCKDOWN,
        CharacterType.MAXIMILIAN
    };
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

    // Unlock manager
    private UnlockManager unlockManager;

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

    // Button textures
    private Texture backgroundTexture;
    private Texture defaultButtonTexture;
    private Texture lockdownButtonTexture;
    private Texture maximilianButtonTexture;
    private Texture soundcloudButtonTexture;
    private Texture confirmButtonTexture;

    // Button texture scale (to make them smaller, like MenuScreen)
    private static final float BUTTON_TEXTURE_SCALE = 0.3f;

    public CharacterSelectionScreen(final Consumer<CharacterType> onCharacterSelected) {
        super(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        this.onCharacterSelected = onCharacterSelected;
    }

    @Override
    public void show() {
        System.out.println("[CharacterSelectionScreen] Showing modern character selection");

        // Note: Assets should already be pre-loaded from SplashScreen
        if (!ResourceManager.getInstance().areAllAssetsLoaded()) {
            System.out.println("[CharacterSelectionScreen] WARNING: Assets not pre-loaded, loading now...");
            ResourceManager.getInstance().preloadAll();
        }

        // Initialize unlock manager
        unlockManager = new UnlockManager();

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

        // Load button textures
        backgroundTexture = new Texture(Gdx.files.internal("img/Speler_selectie.png"));
        defaultButtonTexture = new Texture(Gdx.files.internal("img/Default.png"));
        lockdownButtonTexture = new Texture(Gdx.files.internal("img/Lockdown.png"));
        maximilianButtonTexture = new Texture(Gdx.files.internal("img/Maximilian.png"));
        soundcloudButtonTexture = new Texture(Gdx.files.internal("img/SoundCloud.png"));
        confirmButtonTexture = new Texture(Gdx.files.internal("img/Confirm Selection.png"));

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

        // Fixed hitbox size for buttons (texture is drawn larger but hitbox is smaller)
        float btnWidth = 320;
        float btnHeight = 60;

        final float buttonSpacing = 90; // Spacing between button hitboxes

        // Center buttons in the right half of the screen
        final int rightPanelStart = screenWidth / 2;
        final float rightPanelX = rightPanelStart + (screenWidth / 2f - btnWidth) / 2f - 70; // Moved 70px to the left

        // Position all 4 buttons with consistent spacing from top to bottom
        float topButtonY = 480;
        for (int i = 0; i < characters.length; i++) {
            characterButtons[i] = new Rectangle(
                rightPanelX,
                topButtonY - i * buttonSpacing,
                btnWidth,
                btnHeight
            );
        }

        // Select button dimensions
        float selectBtnWidth = btnWidth;
        float selectBtnHeight = btnHeight;
        if (confirmButtonTexture != null) {
            selectBtnWidth = confirmButtonTexture.getWidth() * BUTTON_TEXTURE_SCALE;
            selectBtnHeight = confirmButtonTexture.getHeight() * BUTTON_TEXTURE_SCALE;
        }

        // Select button at bottom, centered in right half
        selectButton = new Rectangle(
            rightPanelStart + (screenWidth / 2f - selectBtnWidth) / 2f - 75, // Moved slightly to the right
            5, // Even lower on screen
            selectBtnWidth,
            selectBtnHeight
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

        // Render modern UI first
        renderModernUI();

        // Render 3D preview on top on left side
        renderPreview(delta);
    }

    private void handleInput() {
        // Convert screen coordinates to the virtual viewport (handles letterboxing)
        final Vector2 mousePos = getMouseInViewport();
        final float mouseX = mousePos.x;
        final float mouseY = mousePos.y;

        // Check character button hover/click - only changes selection, doesn't start game
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < characterButtons.length; i++) {
                if (characterButtons[i].contains(mouseX, mouseY)) {
                    final CharacterType character = characters[i];
                    final boolean isLocked = !unlockManager.isUnlocked(character);
                    if (!isLocked) {
                        selectedIndex = i;
                        System.out.println("[CharacterSelection] Selected: " + character.getDisplayName() + " (index " + i + ")");
                        break; // Stop after first match
                    }
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
                // Skip locked characters
                int newIndex = (selectedIndex - 1 + characters.length) % characters.length;
                while (!unlockManager.isUnlocked(characters[newIndex]) && newIndex != selectedIndex) {
                    newIndex = (newIndex - 1 + characters.length) % characters.length;
                }
                if (unlockManager.isUnlocked(characters[newIndex])) {
                    selectedIndex = newIndex;
                }
                keyPressed = true;
            } else if (downPressed) {
                // Skip locked characters
                int newIndex = (selectedIndex + 1) % characters.length;
                while (!unlockManager.isUnlocked(characters[newIndex]) && newIndex != selectedIndex) {
                    newIndex = (newIndex + 1) % characters.length;
                }
                if (unlockManager.isUnlocked(characters[newIndex])) {
                    selectedIndex = newIndex;
                }
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

        // Start drawing with SpriteBatch
        batch.begin();

        // Draw background image
        if (backgroundTexture != null) {
            batch.draw(backgroundTexture, 0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        }

        // Calculate hover scales for character buttons and draw textures
        for (int i = 0; i < characterButtons.length; i++) {
            final Rectangle button = characterButtons[i];
            final CharacterType character = characters[i];
            final boolean isLocked = !unlockManager.isUnlocked(character);
            // Smaller hover area - especially vertical (15% horizontal margin, 35% vertical margin)
            final float hoverMarginX = button.width * 0.15f;
            final float hoverMarginY = button.height * 0.35f;
            final boolean isHovered = mouseX >= button.x + hoverMarginX
                && mouseX <= button.x + button.width - hoverMarginX
                && mouseY >= button.y + hoverMarginY
                && mouseY <= button.y + button.height - hoverMarginY
                && !isLocked;
            final boolean isSelected = i == selectedIndex;

            // Base scale per character - Soundcloud and Lockdown are slightly smaller
            float baseScale = 1.0f;
            if (character == CharacterType.SOUNDCLOUD || character == CharacterType.LOCKDOWN) {
                baseScale = 0.87f;
            }

            // Calculate scale: selected skin stays enlarged, hover adds slight extra scale
            float scale = baseScale;
            if (isSelected) {
                scale = baseScale * 1.15f; // Selected skin is always enlarged
            } else if (isHovered) {
                scale = baseScale * 1.10f; // Hover is slightly smaller than selected
            }

            // Dim locked characters (same size, just darker)
            if (isLocked) {
                batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
            } else {
                batch.setColor(1f, 1f, 1f, 1f);
            }

            // Draw character button texture
            final Texture texture = getCharacterTexture(character);
            drawButtonTexture(texture, button, scale);

            // Reset color
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // Draw select button with hover scaling
        final boolean selectHovered = selectButton.contains(mouseX, mouseY);
        final float selectScale = selectHovered ? 1.15f : 1.0f;
        drawButtonTexture(confirmButtonTexture, selectButton, selectScale);

        batch.end();
    }

    /**
     * Draws a button texture with scaling animation centered on the button position.
     * Preserves the original aspect ratio of the texture.
     */
    private void drawButtonTexture(final Texture texture, final Rectangle button, final float scale) {
        if (texture == null) {
            return;
        }
        final float centerX = button.x + button.width / 2f;
        final float centerY = button.y + button.height / 2f;

        // Use texture's original dimensions with base scale and hover scale
        final float originalWidth = texture.getWidth() * BUTTON_TEXTURE_SCALE;
        final float originalHeight = texture.getHeight() * BUTTON_TEXTURE_SCALE;

        final float scaledWidth = originalWidth * scale;
        final float scaledHeight = originalHeight * scale;

        final float scaledX = centerX - scaledWidth / 2f;
        final float scaledY = centerY - scaledHeight / 2f;

        batch.draw(texture, scaledX, scaledY, scaledWidth, scaledHeight);
    }

    /**
     * Gets the texture for a character button based on the character type.
     */
    private Texture getCharacterTexture(final CharacterType type) {
        switch (type) {
            case DEFAULT:
                return defaultButtonTexture;
            case SOUNDCLOUD:
                return soundcloudButtonTexture;
            case LOCKDOWN:
                return lockdownButtonTexture;
            case MAXIMILIAN:
                return maximilianButtonTexture;
            default:
                return null;
        }
    }

    private void initPreviewRenderer() {
        previewBatch = new ModelBatch();
        previewEnvironment = new Environment();
        previewEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        previewEnvironment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        // Camera setup for 3x enlarged model
        previewCamera = new PerspectiveCamera(70f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        previewCamera.position.set(0f, 4.2f, 8.4f);
        previewCamera.lookAt(0f, 3.9f, 0f);
        previewCamera.near = 0.01f;
        previewCamera.far = 100f;
        previewCamera.update();
    }

    private void renderPreview(final float delta) {
        if (previewBatch == null || previewCamera == null) {
            return;
        }

        final CharacterType selected = characters[selectedIndex];

        // Use full window viewport
        final int previewWidth = Gdx.graphics.getWidth();
        final int previewHeight = Gdx.graphics.getHeight();

        // Set GL viewport to full window
        Gdx.gl.glViewport(0, 0, previewWidth, previewHeight);

        // Clear depth buffer
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera viewport dimensions
        previewCamera.viewportWidth = previewWidth;
        previewCamera.viewportHeight = previewHeight;
        previewCamera.update();

        // Load model if needed
        ensurePreviewModel(selected);

        final ModelInstance instance = previewInstances.get(selected);
        final AnimationController controller = previewControllers.get(selected);
        final float modelScale = previewScales.getOrDefault(selected, 1f);
        final float footOffset = previewFootOffsets.getOrDefault(selected, 0f);

        // Apply 3x scale to everything proportionally
        final float enlargedScale = modelScale * 3f;

        // Rotate and position
        previewRotation += delta * 30f;
        instance.transform.idt();
        instance.transform.translate(-4.5f, footOffset * enlargedScale, 0f);
        instance.transform.rotate(Vector3.Y, previewRotation);
        instance.transform.scale(enlargedScale, enlargedScale, enlargedScale);
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

        // Restore viewport for UI
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
        // Check if we already have this model in local cache
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

        // Try to use pre-loaded model from ResourceManager
        final Model preloadedModel = ResourceManager.getInstance().getCharacterModel(type);
        if (preloadedModel != null) {
            // Use pre-loaded model, but create our own instance
            System.out.println("[CharacterSelectionScreen] Using pre-loaded model for " + type.getDisplayName());

            final ModelInstance instance = new ModelInstance(preloadedModel);
            AnimationController controller = null;
            if (preloadedModel.animations.size > 0) {
                controller = new AnimationController(instance);
                controller.setAnimation("walking", -1);
            }

            // Get pre-calculated scale and offset
            final float scale = ResourceManager.getInstance().getCharacterScale(type);
            final float footOffset = ResourceManager.getInstance().getCharacterFootOffset(type);

            // Store in local cache (we don't own the model, so we won't dispose it)
            previewModels.put(type, preloadedModel);
            previewInstances.put(type, instance);
            if (controller != null) {
                previewControllers.put(type, controller);
            }
            previewScales.put(type, scale);
            previewFootOffsets.put(type, footOffset);
            lastPreviewType = type;
            return;
        }

        // Fallback: Load model on-demand if not pre-loaded
        System.out.println("[CharacterSelectionScreen] WARNING: Loading model on-demand for " + type.getDisplayName());
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

        // Try Ch33 (Big Business), Ch06 (Soundcloud), Ch11 (Lockdown), Ch20 (Maximilian), and Ch32 (Default) naming patterns
        final String[] primaries = hair
            ? new String[]{
                "Ch33_1002_" + suffix + ".png",
                "Ch06_1002_" + suffix + ".png",
                "Ch11_1002_" + suffix + ".png",
                "Ch20_1002_" + suffix + ".png",
                "Ch32_1002_" + suffix + ".png"
            }
            : new String[]{
                "Ch33_1001_" + suffix + ".png",
                "Ch06_1001_" + suffix + ".png",
                "Ch11_1001_" + suffix + ".png",
                "Ch20_1001_" + suffix + ".png",
                "Ch32_1001_" + suffix + ".png"
            };

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
        // Dispose button textures
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (defaultButtonTexture != null) {
            defaultButtonTexture.dispose();
        }
        if (lockdownButtonTexture != null) {
            lockdownButtonTexture.dispose();
        }
        if (maximilianButtonTexture != null) {
            maximilianButtonTexture.dispose();
        }
        if (soundcloudButtonTexture != null) {
            soundcloudButtonTexture.dispose();
        }
        if (confirmButtonTexture != null) {
            confirmButtonTexture.dispose();
        }
        // NOTE: Don't dispose previewModels if they came from ResourceManager
        // Only dispose models we loaded ourselves (check if they're different from ResourceManager)
        for (Map.Entry<CharacterType, Model> entry : previewModels.entrySet()) {
            Model model = entry.getValue();
            Model preloadedModel = ResourceManager.getInstance().getCharacterModel(entry.getKey());
            // Only dispose if it's not the pre-loaded one (we loaded it ourselves)
            if (model != preloadedModel && model != null) {
                model.dispose();
            }
        }
        previewModels.clear();
        if (previewFallbackTexture != null) {
            previewFallbackTexture.dispose();
            previewFallbackTexture = null;
        }
    }
}
