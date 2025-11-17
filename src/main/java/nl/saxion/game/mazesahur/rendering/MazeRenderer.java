package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Elevator;
import nl.saxion.game.mazesahur.world.Maze;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles rendering of the 3D maze and entities.
 * Manages models, textures, and rendering pipeline.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class MazeRenderer {

    private final Maze maze;
    private final MaterialManager materialManager;
    private final LightingManager lightingManager;

    private ModelBatch modelBatch;
    private Model wallModel;
    private Model floorModel;
    private Model roofModel;
    private Model enemyModel;
    private Model ceilingLampModel;
    private Model elevatorModel;
    private com.badlogic.gdx.graphics.Texture whiteTexture; // 1x1 white texture for elevator (not used for floor)
    private com.badlogic.gdx.graphics.Texture elevatorFloorTexture; // Floor platform texture (only for Mirror material)
    private Model floorPlatformExtensionModel; // Small platform in front of elevator with maze floor texture
    private ModelInstance floorPlatformExtensionInstance;
    private Model wallLeftModel; // Left wall segment behind elevator
    private Model wallRightModel; // Right wall segment behind elevator
    private Model wallTopModel; // Top wall segment behind elevator
    private ModelInstance wallLeftInstance;
    private ModelInstance wallRightInstance;
    private ModelInstance wallTopInstance;

    private ModelInstance floorInstance;
    private ModelInstance roofInstance;
    private List<ModelInstance> wallInstances;
    private ModelInstance enemyInstance;
    private List<ModelInstance> ceilingLampInstances;
    private List<Vector3> lampLightPositions;
    private List<Boolean> lampIsBroken; // Track which lamps are completely broken
    private ModelInstance elevatorInstance;
    private AnimationController elevatorAnimationController;

    // Lamp flickering
    private float[] lampFlickerTimers;
    private float[] lampFlickerIntensities;
    private int flickerUpdateCounter = 0; // Update every N frames for performance

    /**
     * Creates a new maze renderer.
     *
     * @param maze The game maze
     * @param materialManager Material manager for textures
     * @param lightingManager Lighting manager for shaders
     */
    public MazeRenderer(final Maze maze, final MaterialManager materialManager,
                        final LightingManager lightingManager) {
        this.maze = maze;
        this.materialManager = materialManager;
        this.lightingManager = lightingManager;
        this.wallInstances = new ArrayList<>();
        this.ceilingLampInstances = new ArrayList<>();
        this.lampLightPositions = new ArrayList<>();
        this.lampIsBroken = new ArrayList<>();
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize() {
        // Create model batch with custom shader
        final SpotlightShaderProvider shaderProvider =
            new SpotlightShaderProvider(lightingManager.getShader());
        modelBatch = new ModelBatch(shaderProvider);

        // Build maze geometry
        buildMazeModels();

        // Load enemy model
        loadEnemyModel();

        // Load ceiling lamp models
        loadCeilingLamps();

        // Load elevator model
        loadElevatorModel();
    }

    /**
     * Builds 3D models for walls and floor.
     */
    private void buildMazeModels() {
        final ModelBuilder modelBuilder = new ModelBuilder();

        // Create wall model
        final Material wallMaterial = materialManager.createWallMaterial();
        wallModel = modelBuilder.createBox(
            GameConfig.CELL_SIZE, GameConfig.WALL_HEIGHT, GameConfig.CELL_SIZE,
            wallMaterial,
            VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal
                | VertexAttributes.Usage.TextureCoordinates
        );

        // Create floor model
        final Material floorMaterial = materialManager.createFloorMaterial();
        final float floorSize = maze.getWidth() * GameConfig.CELL_SIZE;
        floorModel = modelBuilder.createBox(
            floorSize, 0.1f, floorSize,
            floorMaterial,
            VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal
                | VertexAttributes.Usage.TextureCoordinates
        );

        // Create floor instance
        floorInstance = new ModelInstance(floorModel);
        floorInstance.transform.setToTranslation(
            floorSize / 2f,
            -0.05f,
            floorSize / 2f
        );

        // Create roof model
        final Material roofMaterial = materialManager.createRoofMaterial();
        roofModel = modelBuilder.createBox(
            floorSize, 0.1f, floorSize,
            roofMaterial,
            VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal
                | VertexAttributes.Usage.TextureCoordinates
        );

        // Create roof instance (positioned at ceiling height)
        roofInstance = new ModelInstance(roofModel);
        roofInstance.transform.setToTranslation(
            floorSize / 2f,
            GameConfig.WALL_HEIGHT + 0.05f,
            floorSize / 2f
        );

        // Create wall instances
        for (int z = 0; z < maze.getHeight(); z++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.isWall(x, z)) {
                    final ModelInstance wallInstance = new ModelInstance(wallModel);
                    wallInstance.transform.setToTranslation(
                        x * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f,
                        GameConfig.WALL_HEIGHT / 2f,
                        z * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f
                    );
                    wallInstances.add(wallInstance);
                }
            }
        }
    }

    /**
     * Loads the enemy 3D model.
     */
    private void loadEnemyModel() {
        final ObjLoader objLoader = new ObjLoader();
        final ObjLoader.ObjLoaderParameters params = new ObjLoader.ObjLoaderParameters();
        params.flipV = true; // IMPORTANT: Fix UV mapping
        enemyModel = objLoader.loadModel(Gdx.files.internal("models/tung tung tung sahur.obj"), params);

        final com.badlogic.gdx.graphics.Texture enemyTexture =
            new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("models/Material.png"), true);
        enemyTexture.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );
        enemyTexture.setWrap(
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge,
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
        );

        enemyInstance = new ModelInstance(enemyModel);

        // Apply material
        final Material enemyMaterial = materialManager.createSahurMaterial(enemyTexture);
        for (final com.badlogic.gdx.graphics.g3d.Material mat : enemyInstance.materials) {
            mat.clear();
            mat.set(enemyMaterial);
        }
    }

    /**
     * Loads ceiling lamp models and places them throughout the maze.
     */
    private void loadCeilingLamps() {
        final ObjLoader objLoader = new ObjLoader();
        final ObjLoader.ObjLoaderParameters params = new ObjLoader.ObjLoaderParameters();
        params.flipV = true; // Fix UV mapping
        ceilingLampModel = objLoader.loadModel(
            Gdx.files.internal("models/broken-ceiling-lamp/source/Lampara.obj"),
            params
        );

        // Load lamp textures (base color and emissive for glowing bulb)
        final com.badlogic.gdx.graphics.Texture lampTexture =
            new com.badlogic.gdx.graphics.Texture(
                Gdx.files.internal("models/broken-ceiling-lamp/textures/DefaultMaterial_Base_color.png"),
                true
            );
        lampTexture.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );

        final com.badlogic.gdx.graphics.Texture lampEmissive =
            new com.badlogic.gdx.graphics.Texture(
                Gdx.files.internal("models/broken-ceiling-lamp/textures/DefaultMaterial_Emissive.png"),
                true
            );
        lampEmissive.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );

        // Place lamps randomly throughout the maze corridors
        final float ceilingHeight = GameConfig.WALL_HEIGHT; // Place at ceiling height
        final float lampScale = 0.025f; // Scale down the lamp model (very small)
        final float lampPlacementChance = 0.25f; // 25% chance per corridor cell
        final float minLampDistance = GameConfig.CELL_SIZE * 4.0f; // Minimum 4 cells apart
        final float brokenLampChance = 0.25f; // 25% of lamps are completely broken (no light)

        final java.util.Random random = new java.util.Random();
        int lampsPlaced = 0;
        int brokenLamps = 0;

        // Iterate through all corridor cells randomly
        for (int z = 0; z < maze.getHeight(); z++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                // Only place lamps in open areas (corridors, not walls)
                if (!maze.isWall(x, z)) {
                    // Random chance to place a lamp
                    if (random.nextFloat() < lampPlacementChance) {
                        // Check if we already placed a lamp very close to this position
                        boolean tooClose = false;
                        final float xPos = x * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;
                        final float zPos = z * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;

                        for (Vector3 existingLamp : lampLightPositions) {
                            float dx = existingLamp.x - xPos;
                            float dz = existingLamp.z - zPos;
                            float distSq = dx * dx + dz * dz;
                            if (distSq < minLampDistance * minLampDistance) {
                                tooClose = true;
                                break;
                            }
                        }

                        if (!tooClose) {
                            final ModelInstance lampInstance = new ModelInstance(ceilingLampModel);

                            // Position lamp at ceiling, centered in cell
                            final float yPos = ceilingHeight * 0.7f; // Hanging down but high enough to walk under

                            lampInstance.transform.setToTranslation(xPos, yPos, zPos);
                            lampInstance.transform.scale(lampScale, lampScale, lampScale);

                            // Determine if this lamp is completely broken
                            final boolean isBroken = random.nextFloat() < brokenLampChance;

                            // Apply textures to lamp materials
                            for (final com.badlogic.gdx.graphics.g3d.Material mat : lampInstance.materials) {
                                mat.clear();
                                // Add diffuse texture
                                mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(lampTexture));
                                // Add emissive texture for glowing effect
                                mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createEmissive(lampEmissive));

                                if (isBroken) {
                                    // Broken lamp - no glow, darker appearance
                                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(
                                        0.0f, 0.0f, 0.0f, 1.0f // No glow
                                    ));
                                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                                        0.3f, 0.3f, 0.3f, 1.0f // Dark/dead lamp
                                    ));
                                    brokenLamps++;
                                } else {
                                    // Working lamp - bright yellow glow
                                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(
                                        2.0f, 1.8f, 1.2f, 1.0f // Bright warm glow (visible with flashlight)
                                    ));
                                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                                        1.0f, 0.95f, 0.6f, 1.0f // Yellow tint
                                    ));
                                }
                            }

                            ceilingLampInstances.add(lampInstance);
                            // Store light position at the lamp bulb (at hanging height)
                            lampLightPositions.add(new Vector3(xPos, yPos + 0.3f, zPos));
                            lampIsBroken.add(isBroken);
                            lampsPlaced++;
                        }
                    }
                }
            }
        }

        System.out.println("[MazeRenderer] Loaded " + ceilingLampInstances.size() + " ceiling lamps (" + brokenLamps + " broken, " + (lampsPlaced - brokenLamps) + " working)");

        // Pass lamp positions to shader for lighting
        setupLampLights();

        // Create and pass maze data to shader for shadow casting
        createMazeShadowTexture();
    }

    /**
     * Creates a texture representing the maze for shadow casting.
     * White pixels = walls, Black pixels = paths
     */
    private void createMazeShadowTexture() {
        final int width = maze.getWidth();
        final int height = maze.getHeight();

        // Create pixel data (RGBA format)
        final com.badlogic.gdx.graphics.Pixmap pixmap =
            new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);

        // Fill pixmap with maze data
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                if (maze.isWall(x, z)) {
                    pixmap.setColor(1, 1, 1, 1); // White = wall
                } else {
                    pixmap.setColor(0, 0, 0, 1); // Black = path
                }
                pixmap.drawPixel(x, z);
            }
        }

        // Create texture from pixmap
        final com.badlogic.gdx.graphics.Texture mazeTexture =
            new com.badlogic.gdx.graphics.Texture(pixmap);
        mazeTexture.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        );

        pixmap.dispose();

        // Pass to shader
        lightingManager.getShader().setMazeData(
            mazeTexture,
            (float) width,
            (float) height,
            GameConfig.CELL_SIZE
        );

        System.out.println("[MazeRenderer] Created maze shadow texture: " + width + "x" + height);
    }

    /**
     * Configures the shader with lamp light positions.
     */
    private void setupLampLights() {
        final int numLights = Math.min(lampLightPositions.size(), 20); // Max 20 lights

        // Initialize flicker arrays
        lampFlickerTimers = new float[numLights];
        lampFlickerIntensities = new float[numLights];

        final Vector3[] positions = new Vector3[numLights];
        final Vector3[] colors = new Vector3[numLights];
        final float[] intensities = new float[numLights];

        for (int i = 0; i < numLights; i++) {
            positions[i] = lampLightPositions.get(i);
            colors[i] = new Vector3(1.0f, 0.85f, 0.5f); // Warm yellow lamp light

            // Broken lamps don't emit light
            if (lampIsBroken.get(i)) {
                intensities[i] = 0.0f; // No light
                lampFlickerIntensities[i] = 0.0f;
            } else {
                intensities[i] = 0.8f; // Subtle base intensity
                lampFlickerIntensities[i] = 0.8f;
            }

            lampFlickerTimers[i] = (float) (Math.random() * 10.0); // Random start times
        }

        lightingManager.getShader().setPointLights(positions, colors, intensities, numLights);
        System.out.println("[MazeRenderer] Configured " + numLights + " lamp lights in shader");
    }

    /**
     * Updates lamp flickering effects with dramatic, broken light variations.
     * Optimized to update less frequently for better performance.
     *
     * @param delta Time since last frame
     */
    public void updateLampFlicker(final float delta) {
        if (lampFlickerTimers == null) return;

        // Only update every 4 frames for responsive broken lamp effect
        flickerUpdateCounter++;
        if (flickerUpdateCounter < 4) {
            return;
        }
        flickerUpdateCounter = 0;

        final int numLights = lampFlickerTimers.length;
        final Vector3[] positions = new Vector3[numLights];
        final Vector3[] colors = new Vector3[numLights];
        final float[] intensities = new float[numLights];

        for (int i = 0; i < numLights; i++) {
            positions[i] = lampLightPositions.get(i);
            colors[i] = new Vector3(1.0f, 0.85f, 0.5f); // Warm yellow

            // Check if this lamp is completely broken
            if (lampIsBroken.get(i)) {
                // Broken lamps stay off (no light)
                intensities[i] = 0.0f;
                lampFlickerIntensities[i] = 0.0f;
            } else {
                // Working lamps flicker dramatically
                lampFlickerTimers[i] += delta * 4; // Compensate for skipped frames

                // Dramatic, broken lamp flicker - erratic variations
                // Each lamp has different flicker pattern (some more broken than others)
                final float brokenLevel = (i % 7) / 7.0f; // How broken this lamp is (0-1)
                final float flickerSpeed = 3.0f + (i % 5) * 1.5f; // Fast, erratic flickering
                final float randomOffset = (float) Math.sin(i * 3.14159f) * 100.0f;

                // Create erratic flicker pattern using multiple sine waves
                float flicker = (float) Math.sin((lampFlickerTimers[i] + randomOffset) * flickerSpeed);

                // Add rapid secondary variation for broken effect
                float variation = (float) Math.sin(lampFlickerTimers[i] * 8.0f + i) * 0.5f;

                // Add random spikes for electrical short effect
                float spike = (float) Math.sin(lampFlickerTimers[i] * 25.0f + i * 7.0f);
                if (spike > 0.85f) {
                    spike = 1.0f; // Sharp on/off spikes
                } else {
                    spike = 0.0f;
                }

                // Combine for dramatic, erratic flickering
                flicker = flicker * 0.6f + variation * 0.3f + spike * 0.3f;

                // Map to intensity range based on how broken the lamp is
                // More broken lamps go darker (0.0 to 0.8) vs less broken (0.4 to 1.2)
                float baseIntensity = 0.6f - brokenLevel * 0.3f;
                float flickerRange = 0.5f + brokenLevel * 0.4f;
                float intensity = baseIntensity + flicker * flickerRange;

                // Some lamps occasionally go completely dark (electrical failure)
                if (brokenLevel > 0.6f) {
                    float darkChance = (float) Math.sin(lampFlickerTimers[i] * 0.7f + i * 3.0f);
                    if (darkChance > 0.9f) {
                        intensity = 0.0f; // Complete failure
                    }
                }

                // Clamp to safe range (allow complete darkness for broken effect)
                intensity = Math.max(0.0f, Math.min(1.2f, intensity));

                lampFlickerIntensities[i] = intensity;
                intensities[i] = intensity;
            }
        }

        lightingManager.getShader().setPointLights(positions, colors, intensities, numLights);
    }

    /**
     * Loads the animated elevator 3D model from GLB file.
     */
    private void loadElevatorModel() {
        try {
            System.out.println("[MazeRenderer] Loading elevator GLB model...");

            // Create a 1x1 white texture to prevent texture bleeding from other objects
            final com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(1.0f, 1.0f, 1.0f, 1.0f); // Pure white
            pixmap.fill();
            whiteTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
            whiteTexture.setFilter(
                com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
            );
            pixmap.dispose();
            System.out.println("[MazeRenderer] Created 1x1 white texture for elevator");

            // Load elevator floor texture
            elevatorFloorTexture = new com.badlogic.gdx.graphics.Texture(
                Gdx.files.internal("models/elevator-animated/542c3de6590e7392990a57b3a76e2b4b_1.jpeg"),
                true // Generate mipmaps
            );
            elevatorFloorTexture.setFilter(
                com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
            );
            elevatorFloorTexture.setWrap(
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat,
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat
            );
            System.out.println("[MazeRenderer] Loaded elevator floor texture");

            // Load GLB file using gdx-gltf library
            final SceneAsset sceneAsset = new GLBLoader().load(
                Gdx.files.internal("models/elevator-animated/ElevatorAnimation.glb")
            );

            // Get the model from the scene
            elevatorModel = sceneAsset.scene.model;
            elevatorInstance = new ModelInstance(elevatorModel);

            // Apply textures: floor platform gets elevator texture, everything else is white
            System.out.println("[MazeRenderer] Applying textures to elevator materials...");
            for (final com.badlogic.gdx.graphics.g3d.Material mat : elevatorInstance.materials) {
                System.out.println("[MazeRenderer] Processing material: " + mat.id);
                // AGGRESSIVE CLEAR: Clear all existing attributes (textures, colors, etc.)
                mat.clear();

                // EXTRA AGGRESSIVE: Remove ALL possible texture attributes to ensure no bleeding
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Normal);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Bump);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Specular);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Emissive);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Reflection);
                mat.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Ambient);

                // Check if this is the floor platform material
                if (mat.id != null && (mat.id.equalsIgnoreCase("Mirror") || mat.id.equalsIgnoreCase("Floor"))) {
                    // This is the floor platform - use ONLY the elevator floor texture (NO white texture)
                    System.out.println("[MazeRenderer]   -> FLOOR PLATFORM - applying ONLY elevator texture (no white)");

                    // Set ONLY the elevator floor texture - do NOT set any other textures
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(elevatorFloorTexture));

                    // Set neutral white color tint (doesn't add extra color)
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                        1.0f, 1.0f, 1.0f, 1.0f // Neutral white tint
                    ));
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createAmbient(
                        0.3f, 0.3f, 0.3f, 1.0f // Lower ambient to see texture better
                    ));

                    System.out.println("[MazeRenderer]   -> Floor material now has ONLY elevator floor texture (no other textures)");
                } else {
                    // Everything else - use white texture ONLY
                    System.out.println("[MazeRenderer]   -> Other part - applying white texture");
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(whiteTexture));
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                        1.0f, 1.0f, 1.0f, 1.0f // Pure white
                    ));
                    mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createAmbient(
                        1.0f, 1.0f, 1.0f, 1.0f // Bright white
                    ));

                    System.out.println("[MazeRenderer]   -> Other material now has ONLY white texture (no other textures)");
                }
            }
            System.out.println("[MazeRenderer] Elevator textures applied (floor = elevator texture, walls = white)");

            // Set up animation controller if animations exist
            if (elevatorModel.animations.size > 0) {
                elevatorAnimationController = new AnimationController(elevatorInstance);
                elevatorAnimationController.setAnimation(elevatorModel.animations.get(0).id, -1);
                System.out.println("[MazeRenderer] Loaded elevator GLB with animation: " + elevatorModel.animations.get(0).id);
                System.out.println("[MazeRenderer] Animation duration: " + elevatorModel.animations.get(0).duration + "s");
            } else {
                System.out.println("[MazeRenderer] Loaded elevator GLB (no animations found)");
            }

            // Create walkable floor layer on top of elevator floor platform with maze floor texture
            System.out.println("[MazeRenderer] Creating walkable floor layer on elevator platform...");
            final ModelBuilder modelBuilder = new ModelBuilder();

            // Clone the floor material so we can modify UV scaling independently
            final Material floorMaterial = materialManager.createFloorMaterial();

            // Create a visible walkable layer - 6 meters wider on each side
            // Make it 0.1 cm thick (0.001 units)
            // Size: 20.0 x 20.0 (8.0 + 6.0 + 6.0) (scaled by 1.5x = 30.0 x 30.0 - extremely wide!)
            floorPlatformExtensionModel = modelBuilder.createBox(
                20.0f, 0.001f, 20.0f,
                floorMaterial,
                VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.TextureCoordinates
            );

            floorPlatformExtensionInstance = new ModelInstance(floorPlatformExtensionModel);

            // Scale UV coordinates to repeat texture properly (tile the texture)
            // The platform is 20.0 units wide (30.0 with scale), so we need a lot of texture repetition
            for (com.badlogic.gdx.graphics.g3d.Material mat : floorPlatformExtensionInstance.materials) {
                // Scale UV by setting texture scale
                final com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute diffuseAttr =
                    mat.get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.class,
                        com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
                if (diffuseAttr != null) {
                    diffuseAttr.scaleU = 3.0f; // Repeat texture 3 times horizontally (even larger tiles)
                    diffuseAttr.scaleV = 3.0f; // Repeat texture 3 times vertically (even larger tiles)
                }
            }

            System.out.println("[MazeRenderer] Walkable floor layer created with maze floor texture and UV scaling");

            // Create wall segments behind elevator with door opening
            System.out.println("[MazeRenderer] Creating wall segments behind elevator with door opening...");

            // Create wall material
            final Material wallMaterial = materialManager.createWallMaterial();

            // Elevator dimensions (scaled 1.5x): width=4.5, height=6.0
            // Create wall segments with door opening in center
            // Door opening: 2.5m wide x 4.0m high (centered)

            // LEFT wall segment: 9.98m wide x 8.0m high x 1cm thick (63cm langer aan beide kanten)
            wallLeftModel = modelBuilder.createBox(
                9.98f, 8.0f, 0.01f,
                wallMaterial.copy(),
                VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.TextureCoordinates
            );

            // RIGHT wall segment: 9.98m wide x 8.0m high x 1cm thick (63cm langer aan beide kanten)
            wallRightModel = modelBuilder.createBox(
                9.98f, 8.0f, 0.01f,
                wallMaterial.copy(),
                VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.TextureCoordinates
            );

            // TOP wall segment: 2.5m wide x 4.0m high x 1cm thick
            wallTopModel = modelBuilder.createBox(
                2.5f, 4.0f, 0.01f,
                wallMaterial.copy(),
                VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.TextureCoordinates
            );

            wallLeftInstance = new ModelInstance(wallLeftModel);
            wallRightInstance = new ModelInstance(wallRightModel);
            wallTopInstance = new ModelInstance(wallTopModel);

            // Apply UV scaling to all wall segments (NO rotation to preserve normals)
            for (ModelInstance instance : new ModelInstance[]{wallLeftInstance, wallRightInstance, wallTopInstance}) {
                for (com.badlogic.gdx.graphics.g3d.Material mat : instance.materials) {
                    final com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute diffuseAttr =
                        mat.get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.class,
                            com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
                    if (diffuseAttr != null) {
                        diffuseAttr.scaleU = 4.0f; // Breder voor de langere segmenten
                        diffuseAttr.scaleV = 3.0f;
                        // NO offsetU - keep normals pointing straight for flashlight
                    }

                    // Add color attributes for proper lighting
                    if (!mat.has(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Diffuse)) {
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(1f, 1f, 1f, 1f));
                    }
                    if (!mat.has(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Ambient)) {
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createAmbient(0.4f, 0.4f, 0.4f, 1f));
                    }
                }
            }

            System.out.println("[MazeRenderer] Wall segments created: Left/Right 8.75m x 8.0m, door opening 2.5m x 4.0m");

        } catch (Exception e) {
            System.err.println("[MazeRenderer] Failed to load elevator GLB model: " + e.getMessage());
            e.printStackTrace();

            // Fallback to simple box
            System.out.println("[MazeRenderer] Creating fallback procedural elevator...");
            final ModelBuilder modelBuilder = new ModelBuilder();
            final Material fallbackMaterial = new Material(
                com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                    1.0f, 1.0f, 1.0f, 1.0f
                ),
                com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createAmbient(
                    1.0f, 1.0f, 1.0f, 1.0f
                )
            );
            elevatorModel = modelBuilder.createBox(
                4.0f, 5.0f, 4.0f,
                fallbackMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
            );
            elevatorInstance = new ModelInstance(elevatorModel);
        }
    }

    /**
     * Renders the elevator entity with animation.
     * DEPRECATED: Use renderWithElevator() instead to prevent material bleeding.
     *
     * @param camera The game camera
     * @param elevator The elevator entity
     */
    public void renderElevator(final PerspectiveCamera camera, final Elevator elevator) {
        // This method is kept for backwards compatibility but does nothing
        // The elevator is now rendered in renderWithElevator() to prevent material bleeding
    }


    /**
     * Renders the maze (walls and floor).
     *
     * @param camera The game camera
     */
    public void render(final PerspectiveCamera camera) {
        modelBatch.begin(camera);
        modelBatch.render(floorInstance);
        modelBatch.render(roofInstance);
        for (final ModelInstance wall : wallInstances) {
            modelBatch.render(wall);
        }
        // Render ceiling lamps
        for (final ModelInstance lamp : ceilingLampInstances) {
            modelBatch.render(lamp);
        }
        modelBatch.end();
    }

    /**
     * Renders the maze and elevator together to prevent material bleeding.
     *
     * @param camera The game camera
     * @param elevator The elevator entity
     */
    public void renderWithElevator(final PerspectiveCamera camera, final Elevator elevator) {
        modelBatch.begin(camera);
        modelBatch.render(floorInstance);
        modelBatch.render(roofInstance);
        for (final ModelInstance wall : wallInstances) {
            modelBatch.render(wall);
        }
        // Render ceiling lamps
        for (final ModelInstance lamp : ceilingLampInstances) {
            modelBatch.render(lamp);
        }

        // Render elevator in the same batch to prevent material bleeding
        if (elevatorInstance != null) {
            // Update animation based on elevator state
            if (elevatorAnimationController != null) {
                final float delta = Gdx.graphics.getDeltaTime();

                // Control animation based on door state
                switch (elevator.getCurrentState()) {
                    case OPENING:
                        elevatorAnimationController.update(delta);
                        break;
                    case CLOSING:
                        elevatorAnimationController.update(-delta); // Reverse
                        break;
                    case OPEN:
                    case CLOSED:
                        // Paused - don't update
                        break;
                }
            }

            // Update elevator transform
            elevatorInstance.transform.idt();
            elevatorInstance.transform.translate(elevator.getPosition());
            elevatorInstance.transform.translate(0, 0, 0); // Ground level

            // Scale to 1.5x size for better visibility
            final float scale = 1.5f;
            elevatorInstance.transform.scale(scale, scale, scale);

            // Render elevator
            modelBatch.render(elevatorInstance);

            // Render walkable floor layer ON TOP of elevator floor platform
            if (floorPlatformExtensionInstance != null) {
                floorPlatformExtensionInstance.transform.idt();
                // Position it raised above the elevator platform - 0.0005 is half the height (0.001/2)
                floorPlatformExtensionInstance.transform.translate(
                    elevator.getPosition().x,
                    0.0005f, // Half the height so bottom sits at ground level
                    elevator.getPosition().z
                );
                // Scale to match the elevator's 1.5x scale
                floorPlatformExtensionInstance.transform.scale(scale, 1.0f, scale);
                modelBatch.render(floorPlatformExtensionInstance);
            }

            // Render wall segments at SIDE of elevator (rotated 90 degrees) with door opening
            final float wallX = elevator.getPosition().x + 0.27f; // Side of elevator (nog 1cm meer naar achter)

            // LEFT wall segment (links van de deur)
            if (wallLeftInstance != null) {
                wallLeftInstance.transform.idt();
                wallLeftInstance.transform.translate(
                    wallX,
                    4.0f, // Center vertically (8.0 height / 2)
                    elevator.getPosition().z - 6.24f // Links, half van 9.98 + half van 2.5 = 4.99 + 1.25 = 6.24
                );
                wallLeftInstance.transform.rotate(com.badlogic.gdx.math.Vector3.Y, 90);
                modelBatch.render(wallLeftInstance);
            }

            // RIGHT wall segment (rechts van de deur)
            if (wallRightInstance != null) {
                wallRightInstance.transform.idt();
                wallRightInstance.transform.translate(
                    wallX,
                    4.0f, // Center vertically (8.0 height / 2)
                    elevator.getPosition().z + 6.24f // Rechts, half van 9.98 + half van 2.5 = 4.99 + 1.25 = 6.24
                );
                wallRightInstance.transform.rotate(com.badlogic.gdx.math.Vector3.Y, 90);
                modelBatch.render(wallRightInstance);
            }

            // TOP wall segment (above door)
            if (wallTopInstance != null) {
                wallTopInstance.transform.idt();
                wallTopInstance.transform.translate(
                    wallX,
                    6.0f, // Above door (4.0m door height + 2.0m segment center)
                    elevator.getPosition().z // Centered horizontally
                );
                wallTopInstance.transform.rotate(com.badlogic.gdx.math.Vector3.Y, 90);
                modelBatch.render(wallTopInstance);
            }
        }

        modelBatch.end();
    }

    /**
     * Renders the enemy entity.
     *
     * @param camera The game camera
     * @param enemy The enemy entity
     */
    public void renderEnemy(final PerspectiveCamera camera, final Enemy enemy) {
        // Update enemy transform - IMPORTANT: order is translate -> rotate -> scale
        enemyInstance.transform.idt(); // Reset transform
        enemyInstance.transform.translate(enemy.getPosition());
        enemyInstance.transform.rotate(Vector3.Y, enemy.getYaw());
        enemyInstance.transform.scale(GameConfig.ENEMY_SCALE, GameConfig.ENEMY_SCALE, GameConfig.ENEMY_SCALE);

        // Render with ESP (no depth test for visibility through walls)
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
        modelBatch.begin(camera);
        modelBatch.render(enemyInstance);
        modelBatch.end();
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (modelBatch != null) modelBatch.dispose();
        if (wallModel != null) wallModel.dispose();
        if (floorModel != null) floorModel.dispose();
        if (roofModel != null) roofModel.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (ceilingLampModel != null) ceilingLampModel.dispose();
        if (elevatorModel != null) elevatorModel.dispose();
        if (floorPlatformExtensionModel != null) floorPlatformExtensionModel.dispose();
        if (wallLeftModel != null) wallLeftModel.dispose();
        if (wallRightModel != null) wallRightModel.dispose();
        if (wallTopModel != null) wallTopModel.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (elevatorFloorTexture != null) elevatorFloorTexture.dispose();
    }
}

