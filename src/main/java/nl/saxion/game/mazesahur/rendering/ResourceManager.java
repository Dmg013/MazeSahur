package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.JsonReader;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.model.CharacterType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton resource manager for pre-loading and caching game assets.
 * Prevents duplicate loading and reduces in-game loading times.
 * Pre-loads materials, character models, enemy models, and audio.
 *
 * @author Tim
 * @version 1.0
 */
public class ResourceManager {
    private static ResourceManager instance;

    // Materials
    private MaterialManager materialManager;
    private boolean materialsLoaded = false;

    // Character models (for selection screen)
    private final Map<CharacterType, Model> characterModels = new EnumMap<>(CharacterType.class);
    private final Map<CharacterType, ModelInstance> characterInstances = new EnumMap<>(CharacterType.class);
    private final Map<CharacterType, AnimationController> characterControllers = new EnumMap<>(CharacterType.class);
    private final Map<CharacterType, Float> characterScales = new EnumMap<>(CharacterType.class);
    private final Map<CharacterType, Float> characterFootOffsets = new EnumMap<>(CharacterType.class);
    private Texture fallbackTexture;
    private boolean charactersLoaded = false;

    // Audio
    private final Map<String, Sound> sounds = new HashMap<>();
    private boolean audioLoaded = false;

    // Loading state
    private boolean isLoading = false;
    private float loadingProgress = 0.0f;
    private String currentLoadingStage = "";

    private ResourceManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of the ResourceManager.
     *
     * @return The ResourceManager instance
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    /**
     * Pre-loads all game assets (materials, character models, audio).
     * This should be called during the splash screen loading phase.
     */
    public void preloadAll() {
        if (isLoading) {
            Gdx.app.log("ResourceManager", "Already loading");
            return;
        }

        isLoading = true;
        loadingProgress = 0.0f;
        long startTime = System.currentTimeMillis();

        Gdx.app.log("ResourceManager", "=== Starting pre-load of all assets ===");

        // Step 1: Load materials (30% of progress)
        preloadMaterialsInternal();
        loadingProgress = 0.3f;

        // Step 2: Load character models (40% of progress)
        preloadCharacterModelsInternal();
        loadingProgress = 0.7f;

        // Step 3: Load audio (30% of progress)
        preloadAudioInternal();
        loadingProgress = 1.0f;

        isLoading = false;
        long duration = System.currentTimeMillis() - startTime;
        Gdx.app.log("ResourceManager", "=== All assets pre-loaded in " + duration + "ms ===");
    }

    /**
     * Pre-loads all materials for the game.
     */
    public void preloadMaterials() {
        if (materialsLoaded) {
            return;
        }
        preloadMaterialsInternal();
    }

    private void preloadMaterialsInternal() {
        if (materialsLoaded) {
            return;
        }

        currentLoadingStage = "Loading materials...";
        Gdx.app.log("ResourceManager", currentLoadingStage);
        long startTime = System.currentTimeMillis();

        if (materialManager == null) {
            materialManager = new MaterialManager();
        }

        materialManager.loadTextures();
        materialsLoaded = true;

        long duration = System.currentTimeMillis() - startTime;
        Gdx.app.log("ResourceManager", "Materials loaded in " + duration + "ms");
    }

    /**
     * Pre-loads all character models for the character selection screen.
     */
    public void preloadCharacterModels() {
        if (charactersLoaded) {
            return;
        }
        preloadCharacterModelsInternal();
    }

    private void preloadCharacterModelsInternal() {
        if (charactersLoaded) {
            return;
        }

        currentLoadingStage = "Loading character models...";
        Gdx.app.log("ResourceManager", currentLoadingStage);
        long startTime = System.currentTimeMillis();

        final G3dModelLoader loader = new G3dModelLoader(new JsonReader());

        for (CharacterType type : CharacterType.values()) {
            try {
                final String basePath = type.getModelPath();
                final String walkingFile = type == CharacterType.DEFAULT ? "WalkingNew" : "Walking";
                final String modelPath = basePath + "/" + walkingFile + ".g3dj";

                final FileHandle modelFile = Gdx.files.internal(modelPath);
                if (!modelFile.exists()) {
                    Gdx.app.error("ResourceManager", "Character model not found: " + modelPath);
                    continue;
                }

                final Model model = loader.loadModel(modelFile, createTextureProvider(modelFile));
                applyMaterialTextures(model, modelFile.parent());

                // Set animation ID
                if (model.animations.size > 0) {
                    final Animation walkingAnim = model.animations.first();
                    walkingAnim.id = "walking";
                }

                // Calculate scale and offset
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

                // Store in cache
                characterModels.put(type, model);
                characterInstances.put(type, instance);
                if (controller != null) {
                    characterControllers.put(type, controller);
                }
                characterScales.put(type, scale);
                characterFootOffsets.put(type, footOffset);

                Gdx.app.log("ResourceManager", "  Loaded character: " + type.getDisplayName());
            } catch (Exception e) {
                Gdx.app.error("ResourceManager", "Failed to load character " + type.getDisplayName(), e);
            }
        }

        charactersLoaded = true;
        long duration = System.currentTimeMillis() - startTime;
        Gdx.app.log("ResourceManager", "Character models loaded in " + duration + "ms");
    }

    /**
     * Pre-loads audio files.
     */
    public void preloadAudio() {
        if (audioLoaded) {
            return;
        }
        preloadAudioInternal();
    }

    private void preloadAudioInternal() {
        if (audioLoaded) {
            return;
        }

        currentLoadingStage = "Loading audio...";
        Gdx.app.log("ResourceManager", currentLoadingStage);
        long startTime = System.currentTimeMillis();

        try {
            // Load flashlight toggle sound
            FileHandle flashlightSound = Gdx.files.internal("audio/light-switch-81967.mp3");
            if (flashlightSound.exists()) {
                sounds.put("flashlight_toggle", Gdx.audio.newSound(flashlightSound));
                Gdx.app.log("ResourceManager", "  Loaded audio: flashlight_toggle");
            }

            FileHandle jumpscareSound = Gdx.files.internal("audio/Jumpscare Sound Effect.mp3");
            if (jumpscareSound.exists()) {
                sounds.put("jumpscare", Gdx.audio.newSound(jumpscareSound));
                Gdx.app.log("ResourceManager", "  Loaded audio: jumpscare");
            }

            FileHandle laughterSound = Gdx.files.internal(
                "audio/SCARY DEMONIC LAUGHTER  Horror Sound Effects  - FREE TO USE.mp3"
            );
            if (laughterSound.exists()) {
                sounds.put("demonic_laughter", Gdx.audio.newSound(laughterSound));
                Gdx.app.log("ResourceManager", "  Loaded audio: demonic_laughter");
            }

            // Add more audio files here as needed
        } catch (Exception e) {
            Gdx.app.error("ResourceManager", "Failed to load audio", e);
        }

        audioLoaded = true;
        long duration = System.currentTimeMillis() - startTime;
        Gdx.app.log("ResourceManager", "Audio loaded in " + duration + "ms");
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
                if (fallbackTexture == null) {
                    fallbackTexture = buildFallbackTexture();
                }
                return fallbackTexture;
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

    /**
     * Gets the cached MaterialManager instance.
     * If materials haven't been pre-loaded, they will be loaded on-demand.
     *
     * @return The MaterialManager instance
     */
    public MaterialManager getMaterialManager() {
        if (!materialsLoaded && !isLoading) {
            Gdx.app.log("ResourceManager", "Materials not pre-loaded, loading on-demand...");
            preloadMaterials();
        }
        return materialManager;
    }

    /**
     * Gets a pre-loaded character model.
     *
     * @param type Character type
     * @return Model or null if not loaded
     */
    public Model getCharacterModel(final CharacterType type) {
        return characterModels.get(type);
    }

    /**
     * Gets a pre-loaded character model instance.
     *
     * @param type Character type
     * @return ModelInstance or null if not loaded
     */
    public ModelInstance getCharacterInstance(final CharacterType type) {
        return characterInstances.get(type);
    }

    /**
     * Gets a pre-loaded character animation controller.
     *
     * @param type Character type
     * @return AnimationController or null if not loaded
     */
    public AnimationController getCharacterController(final CharacterType type) {
        return characterControllers.get(type);
    }

    /**
     * Gets the scale for a character model.
     *
     * @param type Character type
     * @return Scale or 1.0f if not loaded
     */
    public float getCharacterScale(final CharacterType type) {
        return characterScales.getOrDefault(type, 1f);
    }

    /**
     * Gets the foot offset for a character model.
     *
     * @param type Character type
     * @return Foot offset or 0.0f if not loaded
     */
    public float getCharacterFootOffset(final CharacterType type) {
        return characterFootOffsets.getOrDefault(type, 0f);
    }

    /**
     * Gets a pre-loaded sound.
     *
     * @param soundName Sound identifier
     * @return Sound or null if not loaded
     */
    public Sound getSound(final String soundName) {
        return sounds.get(soundName);
    }

    /**
     * Checks if materials have been pre-loaded.
     *
     * @return True if materials are loaded
     */
    public boolean areMaterialsLoaded() {
        return materialsLoaded;
    }

    /**
     * Checks if character models have been pre-loaded.
     *
     * @return True if characters are loaded
     */
    public boolean areCharactersLoaded() {
        return charactersLoaded;
    }

    /**
     * Checks if audio has been pre-loaded.
     *
     * @return True if audio is loaded
     */
    public boolean isAudioLoaded() {
        return audioLoaded;
    }

    /**
     * Checks if all assets are loaded.
     *
     * @return True if all assets are loaded
     */
    public boolean areAllAssetsLoaded() {
        return materialsLoaded && charactersLoaded && audioLoaded;
    }

    /**
     * Checks if resources are currently being loaded.
     *
     * @return True if loading is in progress
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Gets the current loading stage description.
     *
     * @return Loading stage text
     */
    public String getCurrentLoadingStage() {
        return currentLoadingStage;
    }

    /**
     * Gets the loading progress (0.0 to 1.0).
     *
     * @return Loading progress
     */
    public float getLoadingProgress() {
        return loadingProgress;
    }

    /**
     * Disposes of all cached resources.
     * This should be called when the application is shutting down.
     */
    public void dispose() {
        // Dispose materials
        if (materialManager != null) {
            materialManager.dispose();
            materialManager = null;
        }

        // Dispose character models
        for (Model model : characterModels.values()) {
            model.dispose();
        }
        characterModels.clear();
        characterInstances.clear();
        characterControllers.clear();
        characterScales.clear();
        characterFootOffsets.clear();

        // Dispose fallback texture
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
            fallbackTexture = null;
        }

        // Dispose audio
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();

        materialsLoaded = false;
        charactersLoaded = false;
        audioLoaded = false;
        isLoading = false;
        loadingProgress = 0.0f;
        currentLoadingStage = "";

        Gdx.app.log("ResourceManager", "All resources disposed");
    }

    /**
     * Resets the resource manager.
     * Useful for testing or forcing a reload.
     */
    public void reset() {
        dispose();
        Gdx.app.log("ResourceManager", "Resources reset");
    }
}
