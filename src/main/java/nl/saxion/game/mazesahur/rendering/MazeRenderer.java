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

    private ModelInstance floorInstance;
    private List<ModelInstance> wallInstances;
    private ModelInstance enemyInstance;

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
    }
}

