package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
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

    private ModelInstance floorInstance;
    private ModelInstance roofInstance;
    private List<ModelInstance> wallInstances;
    private ModelInstance enemyInstance;
    private List<ModelInstance> ceilingLampInstances;
    private List<Vector3> lampLightPositions;
    private List<Boolean> lampIsBroken; // Track which lamps are completely broken
    private ModelInstance elevatorInstance;
    private AnimationController elevatorAnimController;

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
     * Loads the elevator 3D model with animations from converted G3DB file.
     */
    private void loadElevatorModel() {
        try {
            System.out.println("[MazeRenderer] Loading elevator G3DB model...");

            // Load G3DB model (binary format from fbx-conv)
            // Use UBJsonReader for binary G3DB format
            final com.badlogic.gdx.utils.UBJsonReader ubJsonReader = new com.badlogic.gdx.utils.UBJsonReader();
            final G3dModelLoader g3dLoader = new G3dModelLoader(ubJsonReader);

            // Load model - textures should be in same directory as g3db file
            elevatorModel = g3dLoader.loadModel(
                Gdx.files.internal("models/elevator/source/ElevatorAnimation.g3db")
            );

            elevatorInstance = new ModelInstance(elevatorModel);

            // Make all materials emissive so they glow in the dark
            for (final com.badlogic.gdx.graphics.g3d.Material mat : elevatorInstance.materials) {
                // Add bright emissive glow to all materials
                mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(
                    1.0f, 0.9f, 0.7f, 1.0f  // Warm white glow
                ));
                // Make materials respond to light better
                mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                    1.0f, 1.0f, 1.0f, 1.0f  // Full white diffuse
                ));
            }

            // Initialize animation controller if model has animations
            if (elevatorModel.animations.size > 0) {
                elevatorAnimController = new AnimationController(elevatorInstance);
                System.out.println("[MazeRenderer] Elevator has " + elevatorModel.animations.size + " animations:");
                for (int i = 0; i < elevatorModel.animations.size; i++) {
                    System.out.println("  - " + elevatorModel.animations.get(i).id);
                }
            } else {
                System.out.println("[MazeRenderer] Elevator model has no animations");
            }

            System.out.println("[MazeRenderer] Loaded elevator G3DB model with " + elevatorInstance.materials.size + " materials");

        } catch (Exception e) {
            System.err.println("[MazeRenderer] CRITICAL ERROR loading elevator G3DB: " + e.getMessage());
            e.printStackTrace();

            // Create visible fallback
            System.out.println("[MazeRenderer] Creating fallback box...");
            final ModelBuilder modelBuilder = new ModelBuilder();
            final Material elevatorMaterial = new Material();
            elevatorMaterial.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(1.0f, 0.0f, 0.0f, 1.0f));
            elevatorMaterial.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(2.0f, 0.0f, 0.0f, 1.0f));
            elevatorModel = modelBuilder.createBox(3f, 4f, 3f, elevatorMaterial, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            elevatorInstance = new ModelInstance(elevatorModel);
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
     * Renders the elevator entity with door animation.
     *
     * @param camera The game camera
     * @param elevator The elevator entity
     */
    public void renderElevator(final PerspectiveCamera camera, final Elevator elevator) {
        if (elevatorInstance == null) return;

        // Update animations if controller exists
        if (elevatorAnimController != null) {
            // Update animation based on door open percentage
            final float doorOpenPercentage = elevator.getDoorOpenPercentage();

            // If model has animations, play them based on state
            if (elevatorModel.animations.size > 0) {
                // Set animation time based on door percentage (0-100% of animation)
                final String animId = elevatorModel.animations.get(0).id;
                final float animDuration = elevatorModel.animations.get(0).duration;

                // Manually set animation time instead of playing
                elevatorAnimController.setAnimation(animId, -1); // -1 = loop
                elevatorAnimController.update(doorOpenPercentage * animDuration);
            }
        }

        // Update elevator transform
        elevatorInstance.transform.idt(); // Reset transform

        // Position elevator on the ground
        final Vector3 elevatorPos = elevator.getPosition().cpy();
        elevatorPos.y = 0f; // Floor level
        elevatorInstance.transform.translate(elevatorPos);

        // Scale elevator - FBX models are HUGE, scale down a lot
        final float elevatorScale = 0.05f; // Much smaller!
        elevatorInstance.transform.scale(elevatorScale, elevatorScale, elevatorScale);

        // Render elevator with ESP (no depth test) like enemy so it's always visible
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
        modelBatch.begin(camera);
        modelBatch.render(elevatorInstance);
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
    }
}

