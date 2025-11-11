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
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Enemy;
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
    private Model enemyModel;
    private Model ceilingLampModel;

    private ModelInstance floorInstance;
    private List<ModelInstance> wallInstances;
    private ModelInstance enemyInstance;
    private List<ModelInstance> ceilingLampInstances;
    private List<Vector3> lampLightPositions;

    // Lamp flickering
    private float[] lampFlickerTimers;
    private float[] lampFlickerIntensities;

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

        // Place lamps at regular intervals throughout the maze corridors
        final int lampSpacing = 3; // Place a lamp every 3 cells for more coverage
        final float ceilingHeight = GameConfig.WALL_HEIGHT; // Place at ceiling height
        final float lampScale = 0.05f; // Scale down the lamp model (much smaller)

        // Place lamps in a grid pattern throughout corridors
        for (int z = 2; z < maze.getHeight() - 2; z += lampSpacing) {
            for (int x = 2; x < maze.getWidth() - 2; x += lampSpacing) {
                // Only place lamps in open areas (corridors, not walls)
                if (!maze.isWall(x, z)) {
                    final ModelInstance lampInstance = new ModelInstance(ceilingLampModel);

                    // Position lamp at ceiling, centered in cell
                    final float xPos = x * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;
                    final float yPos = ceilingHeight - 0.2f; // Slightly below ceiling
                    final float zPos = z * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;

                    lampInstance.transform.setToTranslation(xPos, yPos, zPos);
                    lampInstance.transform.scale(lampScale, lampScale, lampScale);

                    // Apply textures to lamp materials with bright yellow glow
                    for (final com.badlogic.gdx.graphics.g3d.Material mat : lampInstance.materials) {
                        mat.clear();
                        // Add diffuse texture
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(lampTexture));
                        // Add emissive texture for glowing effect
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createEmissive(lampEmissive));
                        // Add VERY bright yellow emissive color for glowing bulb
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(
                            3.0f, 2.7f, 1.5f, 1.0f // SUPER BRIGHT yellow glow
                        ));
                        // Add yellow diffuse tint
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                            1.0f, 0.95f, 0.6f, 1.0f // Yellow tint
                        ));
                    }

                    ceilingLampInstances.add(lampInstance);
                    // Store light position (slightly below lamp model for better lighting)
                    lampLightPositions.add(new Vector3(xPos, yPos - 0.1f, zPos));
                }
            }
        }

        // Add additional lamps in between the main grid for better coverage
        for (int z = 2; z < maze.getHeight() - 2; z += lampSpacing) {
            for (int x = 2; x < maze.getWidth() - 2; x += lampSpacing) {
                // Add lamps at offset positions (between grid points)
                final int xOffset = x + lampSpacing / 2;
                final int zOffset = z + lampSpacing / 2;

                if (xOffset < maze.getWidth() - 2 && zOffset < maze.getHeight() - 2 &&
                    !maze.isWall(xOffset, zOffset)) {
                    final ModelInstance lampInstance = new ModelInstance(ceilingLampModel);

                    final float xPos = xOffset * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;
                    final float yPos = ceilingHeight - 0.2f;
                    final float zPos = zOffset * GameConfig.CELL_SIZE + GameConfig.CELL_SIZE / 2f;

                    lampInstance.transform.setToTranslation(xPos, yPos, zPos);
                    lampInstance.transform.scale(lampScale, lampScale, lampScale);

                    // Apply textures to lamp materials with bright yellow glow
                    for (final com.badlogic.gdx.graphics.g3d.Material mat : lampInstance.materials) {
                        mat.clear();
                        // Add diffuse texture
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(lampTexture));
                        // Add emissive texture for glowing effect
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createEmissive(lampEmissive));
                        // Add VERY bright yellow emissive color for glowing bulb
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createEmissive(
                            3.0f, 2.7f, 1.5f, 1.0f // SUPER BRIGHT yellow glow
                        ));
                        // Add yellow diffuse tint
                        mat.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(
                            1.0f, 0.95f, 0.6f, 1.0f // Yellow tint
                        ));
                    }

                    ceilingLampInstances.add(lampInstance);
                    lampLightPositions.add(new Vector3(xPos, yPos - 0.1f, zPos));
                }
            }
        }

        System.out.println("[MazeRenderer] Loaded " + ceilingLampInstances.size() + " ceiling lamps");

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
            intensities[i] = 2.5f; // Base intensity
            lampFlickerTimers[i] = (float) (Math.random() * 10.0); // Random start times
            lampFlickerIntensities[i] = 2.5f;
        }

        lightingManager.getShader().setPointLights(positions, colors, intensities, numLights);
        System.out.println("[MazeRenderer] Configured " + numLights + " lamp lights in shader");
    }

    /**
     * Updates lamp flickering effects with extreme on/off behavior.
     *
     * @param delta Time since last frame
     */
    public void updateLampFlicker(final float delta) {
        if (lampFlickerTimers == null) return;

        final int numLights = lampFlickerTimers.length;
        final Vector3[] positions = new Vector3[numLights];
        final Vector3[] colors = new Vector3[numLights];
        final float[] intensities = new float[numLights];

        for (int i = 0; i < numLights; i++) {
            lampFlickerTimers[i] += delta;

            // EXTREME broken lamp flicker - rapid on/off switching
            // Each lamp has different flicker pattern
            final float flickerSpeed = 12.0f + (i % 5) * 3.0f; // Fast flickering
            final float randomOffset = (float) Math.sin(i * 3.14159f) * 100.0f;

            // Create sharp on/off pattern using sine wave with threshold
            float rawFlicker = (float) Math.sin((lampFlickerTimers[i] + randomOffset) * flickerSpeed);

            // Add secondary fast flutter
            float flutter = (float) Math.sin(lampFlickerTimers[i] * 25.0f + i);

            // Combine for chaotic effect
            rawFlicker += flutter * 0.3f;

            // Convert to hard on/off (no smooth transitions)
            float intensity;
            if (rawFlicker > 0.2f) {
                intensity = 3.5f; // FULL ON
            } else if (rawFlicker > -0.2f) {
                // Random rapid flickering zone
                if (Math.random() < 0.5f) {
                    intensity = 3.5f; // ON
                } else {
                    intensity = 0.0f; // OFF
                }
            } else {
                intensity = 0.0f; // FULL OFF
            }

            // Random complete shutoffs (more frequent)
            if (Math.random() < 0.01f) { // 1% chance per frame
                intensity = 0.0f; // Lamp goes completely off
            }

            // Occasional strobe effect
            if (Math.random() < 0.005f) { // 0.5% chance
                intensity = (Math.random() < 0.5f) ? 3.5f : 0.0f;
            }

            lampFlickerIntensities[i] = intensity;

            positions[i] = lampLightPositions.get(i);
            colors[i] = new Vector3(1.0f, 0.9f, 0.4f); // Bright yellow
            intensities[i] = intensity;
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
     * Disposes rendering resources.
     */
    public void dispose() {
        if (modelBatch != null) modelBatch.dispose();
        if (wallModel != null) wallModel.dispose();
        if (floorModel != null) floorModel.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (ceilingLampModel != null) ceilingLampModel.dispose();
    }
}

