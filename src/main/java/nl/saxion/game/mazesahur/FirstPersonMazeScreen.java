package nl.saxion.game.mazesahur;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * First-person 3D maze game screen.
 * Player can walk through a procedurally generated maze using WASD and mouse look.
 */
public class FirstPersonMazeScreen extends ScalableGameScreen {
    private static final float MOVE_SPEED = 12f;
    private static final float MOUSE_SENSITIVITY = 0.2f;
    private static final float PLAYER_HEIGHT = 3f;
    private static final float WALL_HEIGHT = 8f;
    private static final float WALL_SIZE = 8f; // HUGE walls = very wide corridors
    private static final float COLLISION_RADIUS = 2.5f;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private MazeGenerator maze;
    private List<ModelInstance> wallInstances;
    private ModelInstance floorInstance;
    private Model wallModel;
    private Model floorModel;
    private Model sahurModel;
    private ModelInstance sahurInstance;
    private com.badlogic.gdx.graphics.Texture sahurTexture;

    private final Vector3 playerPosition;
    private final Vector3 sahurPosition;
    private List<int[]> sahurPath;
    private int sahurPathIndex;
    private float sahurPathTimer;
    private static final float SAHUR_SPEED = 4f;
    private static final float SAHUR_SCALE = 0.003f; // Bigger so you can see him
    private static final float PATH_UPDATE_INTERVAL = 1f;
    private float pathUpdateTimer;

    private float yaw;
    private float pitch;
    private int lastMouseX;
    private int lastMouseY;
    private boolean firstMouse;

    /**
     * Creates a new first-person maze screen.
     */
    public FirstPersonMazeScreen() {
        super(1280, 720);
        playerPosition = new Vector3(12f, PLAYER_HEIGHT, 12f); // Start in open area
        sahurPosition = new Vector3(100f, PLAYER_HEIGHT, 100f); // Start far away in open path
        sahurPath = new ArrayList<>();
        sahurPathIndex = 0;
        sahurPathTimer = 0;
        pathUpdateTimer = 0;
        yaw = 0;
        pitch = 0;
        firstMouse = true;
    }

    @Override
    public void show() {
        // Load UI font
        GameApp.addFont("ui", "fonts/basic.ttf", 24);

        // Hide cursor and capture it for FPS controls
        Gdx.input.setCursorCatched(true);

        // Setup camera
        final int screenWidth = Gdx.graphics.getBackBufferWidth();
        final int screenHeight = Gdx.graphics.getBackBufferHeight();
        camera = new PerspectiveCamera(67, screenWidth, screenHeight);
        camera.position.set(playerPosition);
        camera.lookAt(playerPosition.x, playerPosition.y, playerPosition.z - 1);
        camera.near = 0.01f;
        camera.far = 100f;
        camera.update();

        // Setup rendering
        modelBatch = new ModelBatch();

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -1f, -0.5f));

        // Generate LARGE maze with wide corridors
        final int mazeSize = 25; // Much larger maze = longer to solve
        maze = new MazeGenerator(mazeSize, mazeSize, System.currentTimeMillis());
        maze.generate();

        // Build 3D models
        buildMazeModels();

        // Load Sahur model
        loadSahurModel();
    }

    /**
     * Builds 3D models for the maze walls and floor.
     */
    private void buildMazeModels() {
        final ModelBuilder modelBuilder = new ModelBuilder();

        // Create wall model (bigger cube - 2x2 instead of 1x1)
        final Material wallMaterial = new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY));
        wallModel = modelBuilder.createBox(WALL_SIZE, WALL_HEIGHT, WALL_SIZE, wallMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Create floor model (flat square - scaled up)
        final Material floorMaterial = new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));
        final float floorSize = maze.getWidth() * WALL_SIZE;
        floorModel = modelBuilder.createBox(floorSize, 0.1f, floorSize, floorMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Create floor instance
        floorInstance = new ModelInstance(floorModel);
        floorInstance.transform.setToTranslation(
                floorSize / 2f,
                -0.05f,
                floorSize / 2f
        );

        // Create wall instances (scaled to 2x2 grid)
        wallInstances = new ArrayList<>();
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.isWall(x, y)) {
                    final ModelInstance wallInstance = new ModelInstance(wallModel);
                    wallInstance.transform.setToTranslation(
                            x * WALL_SIZE + WALL_SIZE / 2f,
                            WALL_HEIGHT / 2f,
                            y * WALL_SIZE + WALL_SIZE / 2f
                    );
                    wallInstances.add(wallInstance);
                }
            }
        }
    }

    /**
     * Loads the Sahur 3D model with texture.
     */
    private void loadSahurModel() {
        // Load OBJ model
        final ObjLoader objLoader = new ObjLoader();
        final ObjLoader.ObjLoaderParameters params = new ObjLoader.ObjLoaderParameters();
        params.flipV = true;
        sahurModel = objLoader.loadModel(Gdx.files.internal("models/tung tung tung sahur.obj"), params);

        // Load texture
        sahurTexture = new com.badlogic.gdx.graphics.Texture(
            Gdx.files.internal("models/Material.png"),
            true
        );
        sahurTexture.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );
        sahurTexture.setWrap(
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge,
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
        );

        // Create instance
        sahurInstance = new ModelInstance(sahurModel);

        // Apply texture
        for (final com.badlogic.gdx.graphics.g3d.Material material : sahurInstance.materials) {
            material.set(TextureAttribute.createDiffuse(sahurTexture));
        }

        // Set initial position and scale
        updateSahurTransform();
    }

    /**
     * Updates Sahur's transform (position, scale, rotation).
     */
    private void updateSahurTransform() {
        sahurInstance.transform.setToScaling(SAHUR_SCALE, SAHUR_SCALE, SAHUR_SCALE);
        sahurInstance.transform.setTranslation(sahurPosition);
    }

    @Override
    public void render(final float delta) {
        handleInput(delta);
        updateSahurAI(delta);
        updateCamera();

        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render 3D maze
        modelBatch.begin(camera);
        modelBatch.render(floorInstance, environment);
        for (final ModelInstance wall : wallInstances) {
            modelBatch.render(wall, environment);
        }
        modelBatch.end();

        // Render Sahur with ESP (no depth test) so you can see him through walls
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        modelBatch.begin(camera);
        modelBatch.render(sahurInstance, environment);
        modelBatch.end();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // Draw 2D UI
        GameApp.startSpriteRendering();
        GameApp.drawText("ui", "MazeSahur - First Person", 20, getWorldHeight() - 20, "white");
        GameApp.drawText("ui", "WASD to move, Mouse to look", 20, getWorldHeight() - 50, "white");

        // Show Sahur distance for debugging
        final float distance = playerPosition.dst(sahurPosition);
        GameApp.drawText("ui", "Sahur distance: " + (int)distance, 20, getWorldHeight() - 80, "red-500");

        GameApp.drawText("ui", "ESC to exit", 20, 30, "amber-500");
        GameApp.endSpriteRendering();

        // Handle exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
            Gdx.app.exit();
        }
    }

    /**
     * Handles player input for movement and camera rotation.
     *
     * @param delta Time since last frame
     */
    private void handleInput(final float delta) {
        // Mouse look
        final int mouseX = Gdx.input.getX();
        final int mouseY = Gdx.input.getY();

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
        }

        final float deltaX = (mouseX - lastMouseX) * MOUSE_SENSITIVITY;
        final float deltaY = (mouseY - lastMouseY) * MOUSE_SENSITIVITY;

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        yaw += deltaX;
        pitch -= deltaY;

        // Clamp pitch
        final float maxPitch = 89f;
        if (pitch > maxPitch) {
            pitch = maxPitch;
        }
        if (pitch < -maxPitch) {
            pitch = -maxPitch;
        }

        // WASD movement
        final Vector3 forward = getForwardVector();
        final Vector3 right = getRightVector();
        final Vector3 moveDirection = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveDirection.add(forward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveDirection.sub(forward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveDirection.sub(right);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveDirection.add(right);
        }

        // Normalize and apply movement
        if (moveDirection.len() > 0) {
            moveDirection.nor().scl(MOVE_SPEED * delta);

            // Sliding collision: try full movement, then X only, then Z only
            final Vector3 newPosition = playerPosition.cpy().add(moveDirection);

            if (!checkCollision(newPosition)) {
                // No collision, move freely
                playerPosition.set(newPosition);
            } else {
                // Try moving only in X direction (slide along Z wall)
                final Vector3 slideX = playerPosition.cpy().add(moveDirection.x, 0, 0);
                if (!checkCollision(slideX)) {
                    playerPosition.set(slideX);
                } else {
                    // Try moving only in Z direction (slide along X wall)
                    final Vector3 slideZ = playerPosition.cpy().add(0, 0, moveDirection.z);
                    if (!checkCollision(slideZ)) {
                        playerPosition.set(slideZ);
                    }
                    // If both fail, player is stuck in corner and doesn't move
                }
            }
        }
    }

    /**
     * Updates Sahur's AI to chase the player using pathfinding.
     *
     * @param delta Time since last frame
     */
    private void updateSahurAI(final float delta) {
        // Update path periodically
        pathUpdateTimer += delta;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            pathUpdateTimer = 0;

            // Calculate grid positions
            final int sahurGridX = (int) (sahurPosition.x / WALL_SIZE);
            final int sahurGridZ = (int) (sahurPosition.z / WALL_SIZE);
            final int playerGridX = (int) (playerPosition.x / WALL_SIZE);
            final int playerGridZ = (int) (playerPosition.z / WALL_SIZE);

            // Find new path
            sahurPath = PathFinder.findPath(maze, sahurGridX, sahurGridZ, playerGridX, playerGridZ);
            sahurPathIndex = 0;
        }

        // Follow path
        if (sahurPath != null && !sahurPath.isEmpty() && sahurPathIndex < sahurPath.size()) {
            final int[] targetGrid = sahurPath.get(sahurPathIndex);
            final float targetX = targetGrid[0] * WALL_SIZE + WALL_SIZE / 2f;
            final float targetZ = targetGrid[1] * WALL_SIZE + WALL_SIZE / 2f;

            // Move towards target
            final Vector3 direction = new Vector3(targetX - sahurPosition.x, 0, targetZ - sahurPosition.z);
            final float distance = direction.len();

            if (distance < 1f) {
                // Reached waypoint, move to next
                sahurPathIndex++;
            } else {
                // Move towards waypoint
                direction.nor().scl(SAHUR_SPEED * delta);
                sahurPosition.add(direction);
            }
        }

        // Update Sahur's transform
        updateSahurTransform();
    }

    /**
     * Gets the forward direction vector based on yaw.
     *
     * @return Forward vector
     */
    private Vector3 getForwardVector() {
        final double radians = Math.toRadians(yaw);
        return new Vector3(
                (float) Math.sin(radians),
                0,
                -(float) Math.cos(radians)
        );
    }

    /**
     * Gets the right direction vector based on yaw.
     *
     * @return Right vector
     */
    private Vector3 getRightVector() {
        final double radians = Math.toRadians(yaw + 90);
        return new Vector3(
                (float) Math.sin(radians),
                0,
                -(float) Math.cos(radians)
        );
    }

    /**
     * Updates camera position and rotation based on player state.
     */
    private void updateCamera() {
        camera.position.set(playerPosition);

        // Calculate look direction
        final double yawRad = Math.toRadians(yaw);
        final double pitchRad = Math.toRadians(pitch);

        final float lookX = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        final float lookY = (float) Math.sin(pitchRad);
        final float lookZ = -(float) (Math.cos(pitchRad) * Math.cos(yawRad));

        camera.direction.set(lookX, lookY, lookZ).nor();
        camera.update();
    }

    /**
     * Checks if the given position collides with any walls.
     *
     * @param position Position to check
     * @return True if collision detected
     */
    private boolean checkCollision(final Vector3 position) {
        // Check collision with circle vs AABB (accurate method)
        final int gridX = (int) Math.floor(position.x / WALL_SIZE);
        final int gridZ = (int) Math.floor(position.z / WALL_SIZE);

        // Check 3x3 grid around player position
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                final int checkX = gridX + dx;
                final int checkZ = gridZ + dz;

                // Check if this grid cell is a wall
                if (checkX >= 0 && checkX < maze.getWidth()
                    && checkZ >= 0 && checkZ < maze.getHeight()
                    && maze.isWall(checkX, checkZ)) {

                    // Wall bounds in world space
                    final float wallMinX = checkX * WALL_SIZE;
                    final float wallMaxX = wallMinX + WALL_SIZE;
                    final float wallMinZ = checkZ * WALL_SIZE;
                    final float wallMaxZ = wallMinZ + WALL_SIZE;

                    // Find closest point on wall box to player circle center
                    final float closestX = Math.max(wallMinX, Math.min(position.x, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(position.z, wallMaxZ));

                    // Calculate distance from player to closest point
                    final float dx2 = position.x - closestX;
                    final float dz2 = position.z - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;

                    // Collision if distance is less than collision radius
                    if (distSquared < COLLISION_RADIUS * COLLISION_RADIUS) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void resize(final int width, final int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        GameApp.disposeFont("ui");
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        if (wallModel != null) {
            wallModel.dispose();
        }
        if (floorModel != null) {
            floorModel.dispose();
        }
        if (sahurModel != null) {
            sahurModel.dispose();
        }
        if (sahurTexture != null) {
            sahurTexture.dispose();
        }
    }

    @Override
    public void dispose() {
        hide();
    }
}
