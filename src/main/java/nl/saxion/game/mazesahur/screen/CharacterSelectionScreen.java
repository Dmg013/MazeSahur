package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
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
 * Character selection screen where players choose their skin/model.
 * Displayed before entering the game.
 *
 * @author Tim
 * @version 1.0
 */
public class CharacterSelectionScreen implements Screen {
    private static final String FONT_NAME = "ui";
    private static final int FONT_SIZE = 32;

    private int selectedIndex = 0;
    private final CharacterType[] characters = CharacterType.values();
    private boolean keyPressed = false;
    private CharacterType lastPreviewType = null;
    private float previewRotation = 0f;

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
        this.onCharacterSelected = onCharacterSelected;
    }

    @Override
    public void show() {
        System.out.println("[CharacterSelectionScreen] Showing character selection");
        GameApp.addFont(FONT_NAME, "fonts/basic.ttf", FONT_SIZE);

        initPreviewRenderer();

        // Load saved character selection
        final String savedCharacter = Gdx.app.getPreferences("MazeSahur").getString("selectedCharacter", "DEFAULT");
        for (int i = 0; i < characters.length; i++) {
            if (characters[i].name().equals(savedCharacter)) {
                selectedIndex = i;
                break;
            }
        }
    }

    @Override
    public void render(float delta) {
        // Clear screen with dark background
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Handle input
        handleInput();

        // Render preview before UI so depth buffer is correct
        renderPreview(delta);

        // Render UI
        renderUI();
    }

    private void handleInput() {
        // Prevent key repeat - only register on key press, not hold
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

        // Reset key pressed flag when all keys are released
        if (!upPressed && !downPressed && !enterPressed) {
            keyPressed = false;
        }
    }

    private void renderUI() {
        final int centerX = Gdx.graphics.getWidth() / 2;
        final int startY = Gdx.graphics.getHeight() - 100;

        // Begin sprite rendering for text drawing
        GameApp.startSpriteRendering();

        // Title
        GameApp.drawText(FONT_NAME, "SELECT YOUR CHARACTER", centerX, startY, "white");

        // Character list
        int yOffset = startY - 100;
        for (int i = 0; i < characters.length; i++) {
            final CharacterType character = characters[i];
            final boolean isSelected = i == selectedIndex;

            // Selection indicator and character name
            final String prefix = isSelected ? "> " : "  ";
            final String text = prefix + character.getDisplayName();
            final String color = isSelected ? "cyan-400" : "gray-400";

            GameApp.drawText(FONT_NAME, text, centerX, yOffset, color);

            yOffset -= isSelected ? 80 : 60;
        }

        // Instructions
        GameApp.drawText(FONT_NAME, "Use Arrow Keys or W/S to navigate", centerX, 120, "gray-500");
        GameApp.drawText(FONT_NAME, "Press ENTER or SPACE to select", centerX, 80, "gray-500");

        // Preview info
        final CharacterType selected = characters[selectedIndex];
        final String previewText = "Character: " + selected.getDisplayName();
        GameApp.drawText(FONT_NAME, previewText, centerX, 40, "green-400");

        // Finish sprite rendering
        GameApp.endSpriteRendering();
    }

    private void initPreviewRenderer() {
        previewBatch = new ModelBatch();
        previewEnvironment = new Environment();
        previewEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        previewEnvironment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        previewCamera = new PerspectiveCamera(50f, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight());
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

        // Update viewport for left side preview
        final int previewWidth = Gdx.graphics.getWidth() / 2;
        final int previewHeight = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, previewWidth, previewHeight);
        previewCamera.viewportWidth = previewWidth;
        previewCamera.viewportHeight = previewHeight;
        previewCamera.update();

        // Load model if needed
        ensurePreviewModel(selected);

        final ModelInstance instance = previewInstances.get(selected);
        final AnimationController controller = previewControllers.get(selected);
        final float scale = previewScales.getOrDefault(selected, 1f);
        final float footOffset = previewFootOffsets.getOrDefault(selected, 0f);

        // Rotate and position
        previewRotation += delta * 30f; // degrees per second
        instance.transform.idt();
        instance.transform.translate(0f, footOffset * scale, 0f);
        instance.transform.rotate(Vector3.Y, previewRotation);
        instance.transform.scale(scale, scale, scale);
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

        // Restore full viewport for UI
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void ensurePreviewModel(final CharacterType type) {
        if (previewModels.containsKey(type)) {
            if (lastPreviewType != type) {
                // Restart animation when switching
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

        // Ensure the walking animation is named consistently
        if (model.animations.size > 0) {
            final Animation walkingAnim = model.animations.first();
            walkingAnim.id = "walking";
        }

        // Calculate scale and foot offset similar to MazeRenderer
        final ModelInstance tempInstance = new ModelInstance(model);
        final BoundingBox bounds = new BoundingBox();
        tempInstance.calculateBoundingBox(bounds);
        final Vector3 dimensions = bounds.getDimensions(new Vector3());
        float scale = 1f;
        float footOffset = -bounds.min.y;
        if (dimensions.y > 0.0001f) {
            scale = (GameConfig.PLAYER_HEIGHT / dimensions.y) * 1.1f;
        }

        // Create instance and controller
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
                mat.set(TextureAttribute.createDiffuse(new Texture(diffuseFile)));
            }

            final FileHandle normalFile = pickTexture(baseDir, isHair, "normal");
            if (normalFile != null) {
                mat.set(TextureAttribute.createNormal(new Texture(normalFile)));
            }

            final FileHandle specFile = pickTexture(baseDir, isHair, "specular");
            if (specFile != null) {
                mat.set(TextureAttribute.createSpecular(new Texture(specFile)));
            }
        }
    }

    private FileHandle pickTexture(final FileHandle baseDir, final boolean hair, final String key) {
        final String suffix = key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
        final String primary = hair ? "Ch33_1002_" + suffix + ".png" : "Ch33_1001_" + suffix + ".png";
        FileHandle handle = baseDir.child(primary);
        if (handle.exists()) {
            return handle;
        }
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

    /**
     * Gets the currently selected character type.
     *
     * @return Selected character type
     */
    public CharacterType getSelectedCharacter() {
        return characters[selectedIndex];
    }

    /**
     * Gets the saved character selection from preferences.
     *
     * @return Saved character type, or DEFAULT if none saved
     */
    public static CharacterType getSavedCharacter() {
        final String savedName = Gdx.app.getPreferences("MazeSahur").getString("selectedCharacter", "DEFAULT");
        return CharacterType.fromString(savedName);
    }

    @Override
    public void resize(int width, int height) {
        // Not needed for this simple UI
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
        // Not needed
    }

    @Override
    public void dispose() {
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
