package nl.saxion.game.mazesahur;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private static final float SAHUR_COLLISION_RADIUS = 3.5f; // Wider than player for smooth corner navigation

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private LightingManager lightingManager;
    private MaterialManager materialManager;
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
    private float sahurYaw; // Direction Sahur is facing
    private List<int[]> sahurPath;
    private int sahurPathIndex;
    private float sahurPathTimer;
    private static final float SAHUR_SPEED = 4f;
    private static final float SAHUR_SCALE = 0.005f; // Bigger so you can see him better
    private static final float SAHUR_HEIGHT = 0.5f; // Height above ground
    private static final float PATH_UPDATE_INTERVAL = 3f; // Only pathfind every 3 seconds
    private static final float WANDER_UPDATE_INTERVAL = 2f; // Change wander direction every 2 seconds
    private static final float CHASE_MEMORY_DURATION = 10f; // Remember player position for 10 seconds
    private float pathUpdateTimer;
    private float wanderUpdateTimer;
    private float timeSincePlayerSeen; // How long since we last saw the player
    private Vector3 lastKnownPlayerPosition; // Where we last saw the player
    private Vector3 wanderTarget;
    private final Random random;

    // Movement smoothing to avoid getting stuck
    private Vector3 lastPosition;
    private float stuckTimer;
    private static final float STUCK_THRESHOLD = 0.5f; // If haven't moved much in 0.5 seconds, we're stuck

    // AI State
    private enum AIState {
        WANDERING,   // Moving randomly
        CHASING,     // Direct line of sight to player
        PURSUING,    // Going to last known position
        PATHFINDING  // Using A* to navigate maze
    }
    private AIState aiState;

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
        sahurPosition = new Vector3(0f, SAHUR_HEIGHT, 0f); // Will be set in show()
        sahurYaw = 0;
        sahurPath = new ArrayList<>();
        sahurPathIndex = 0;
        sahurPathTimer = 0;
        pathUpdateTimer = 0;
        wanderUpdateTimer = 0;
        timeSincePlayerSeen = CHASE_MEMORY_DURATION + 1; // Start with no memory
        lastKnownPlayerPosition = new Vector3();
        wanderTarget = new Vector3();
        lastPosition = new Vector3();
        stuckTimer = 0;
        random = new Random();
        aiState = AIState.WANDERING;
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

        // Initialize lighting manager for horror atmosphere
        lightingManager = new LightingManager();

        // Setup rendering with custom shader
        final SpotlightShaderProvider shaderProvider = new SpotlightShaderProvider(lightingManager.getShader());
        modelBatch = new ModelBatch(shaderProvider);

        // Initialize material manager
        materialManager = new MaterialManager();
        materialManager.loadTextures();

        // Generate LARGE maze with wide corridors
        final int mazeSize = 25; // Much larger maze = longer to solve
        maze = new MazeGenerator(mazeSize, mazeSize, System.currentTimeMillis());
        maze.generate();

        // Find valid spawn position for Sahur (far from player)
        findValidSahurSpawn();

        // Build 3D models
        buildMazeModels();

        // Load Sahur model
        loadSahurModel();

        // Initialize first wander target
        updateWanderTarget();
    }

    /**
     * Finds a valid spawn position for Sahur that's far from the player.
     */
    private void findValidSahurSpawn() {
        final int playerGridX = (int) (playerPosition.x / WALL_SIZE);
        final int playerGridZ = (int) (playerPosition.z / WALL_SIZE);

        int bestX = -1;
        int bestZ = -1;
        float maxDistance = 0;

        // Search for the furthest valid position from player
        for (int z = 0; z < maze.getHeight(); z++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                // Skip walls
                if (maze.isWall(x, z)) {
                    continue;
                }

                // Check that this position is surrounded by valid space (not stuck in corner)
                int validNeighbors = 0;
                final int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
                for (final int[] dir : directions) {
                    final int nx = x + dir[0];
                    final int nz = z + dir[1];
                    if (nx >= 0 && nx < maze.getWidth() && nz >= 0 && nz < maze.getHeight()
                        && !maze.isWall(nx, nz)) {
                        validNeighbors++;
                    }
                }

                // Need at least 2 valid neighbors to ensure it's not a dead end
                if (validNeighbors < 2) {
                    continue;
                }

                // Calculate distance from player
                final float dx = x - playerGridX;
                final float dz = z - playerGridZ;
                final float distance = (float) Math.sqrt(dx * dx + dz * dz);

                // Keep track of furthest valid position
                if (distance > maxDistance) {
                    maxDistance = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
        }

        // If we found a valid position, use it
        if (bestX >= 0 && bestZ >= 0) {
            sahurPosition.set(
                bestX * WALL_SIZE + WALL_SIZE / 2f,
                SAHUR_HEIGHT,
                bestZ * WALL_SIZE + WALL_SIZE / 2f
            );
            lastPosition.set(sahurPosition);
            System.out.println("Sahur spawned at grid (" + bestX + ", " + bestZ + ") - "
                + (int)maxDistance + " cells from player");
        } else {
            // Fallback to a safer default
            sahurPosition.set(
                maze.getWidth() * WALL_SIZE / 2f,
                SAHUR_HEIGHT,
                maze.getHeight() * WALL_SIZE / 2f
            );
            lastPosition.set(sahurPosition);
            System.out.println("Sahur spawned at maze center (fallback)");
        }
    }

    /**
     * Builds 3D models for the maze walls and floor.
     */
    private void buildMazeModels() {
        final ModelBuilder modelBuilder = new ModelBuilder();

        // Create wall model with realistic material
        final Material wallMaterial = materialManager.createWallMaterial();
        wallModel = modelBuilder.createBox(WALL_SIZE, WALL_HEIGHT, WALL_SIZE, wallMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

        // Create floor model with realistic material
        final Material floorMaterial = materialManager.createFloorMaterial();
        final float floorSize = maze.getWidth() * WALL_SIZE;
        floorModel = modelBuilder.createBox(floorSize, 0.1f, floorSize, floorMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

        // Create floor instance
        floorInstance = new ModelInstance(floorModel);
        floorInstance.transform.setToTranslation(
                floorSize / 2f,
                -0.05f,
                floorSize / 2f
        );

        // Scale texture coordinates to repeat the texture across the floor
        final float textureScale = floorSize / (WALL_SIZE * 2f);
        for (final com.badlogic.gdx.graphics.g3d.Material mat : floorInstance.materials) {
            final TextureAttribute texAttr = (TextureAttribute) mat.get(TextureAttribute.Diffuse);
            if (texAttr != null) {
                texAttr.scaleU = textureScale;
                texAttr.scaleV = textureScale;
            }
        }

        // Create wall instances
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

        // Create instance with horror material
        sahurInstance = new ModelInstance(sahurModel);

        // Apply material from material manager
        final Material sahurMaterial = materialManager.createSahurMaterial(sahurTexture);
        for (final com.badlogic.gdx.graphics.g3d.Material mat : sahurInstance.materials) {
            mat.clear();
            mat.set(sahurMaterial);
        }

        // Set initial position and scale
        updateSahurTransform();
    }

    /**
     * Updates Sahur's transform (position, scale, rotation).
     */
    private void updateSahurTransform() {
        sahurInstance.transform.idt(); // Reset transform
        sahurInstance.transform.translate(sahurPosition);
        sahurInstance.transform.rotate(Vector3.Y, sahurYaw); // Rotate to face direction
        sahurInstance.transform.scale(SAHUR_SCALE, SAHUR_SCALE, SAHUR_SCALE);
    }

    @Override
    public void render(final float delta) {
        handleInput(delta);
        updateSahurAI(delta);
        updateCamera();

        // Check if player is moving for flashlight bobbing effect
        final boolean isMoving = Gdx.input.isKeyPressed(Input.Keys.W)
            || Gdx.input.isKeyPressed(Input.Keys.A)
            || Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.D);

        // Update lighting (flashlight follows player with bobbing)
        lightingManager.updateFlashlight(playerPosition, camera.direction, delta, isMoving);

        // Clear screen with pure black (dark horror atmosphere)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render 3D maze with custom shader (handled by ShaderProvider)
        modelBatch.begin(camera);
        modelBatch.render(floorInstance);
        for (final ModelInstance wall : wallInstances) {
            modelBatch.render(wall);
        }
        modelBatch.end();

        // Render Sahur with ESP (no depth test) so you can see him through walls
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        modelBatch.begin(camera);
        modelBatch.render(sahurInstance);
        modelBatch.end();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // Draw 2D UI
        GameApp.startSpriteRendering();
        GameApp.drawText("ui", "MazeSahur - First Person", 20, getWorldHeight() - 20, "white");
        GameApp.drawText("ui", "WASD to move, Mouse to look, F to toggle flashlight", 20, getWorldHeight() - 50, "white");

        // Show flashlight status
        final String flashlightStatus = lightingManager.isFlashlightEnabled() ? "ON" : "OFF";
        final String flashlightColor = lightingManager.isFlashlightEnabled() ? "green-500" : "red-500";
        GameApp.drawText("ui", "Flashlight: " + flashlightStatus, 20, getWorldHeight() - 80, flashlightColor);

        // Show Sahur debug info
        final float distance = playerPosition.dst(sahurPosition);
        GameApp.drawText("ui", "Sahur distance: " + (int)distance, 20, getWorldHeight() - 110, "red-500");
        GameApp.drawText("ui", "AI State: " + aiState, 20, getWorldHeight() - 140, "amber-500");

        if (aiState == AIState.PURSUING) {
            final int timeRemaining = (int) (CHASE_MEMORY_DURATION - timeSincePlayerSeen);
            GameApp.drawText("ui", "Pursuit time: " + timeRemaining + "s", 20, getWorldHeight() - 170, "red-500");
        }

        GameApp.drawText("ui", "ESC to exit", 20, 30, "amber-500");
        GameApp.endSpriteRendering();

        // Handle flashlight toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            lightingManager.toggleFlashlight();
        }

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
     * Updates Sahur's AI with intelligent behavior:
     * - Chases directly when player is visible
     * - Pursues last known position for 10 seconds after losing sight
     * - Wanders randomly when player not visible
     * - Uses pathfinding occasionally to get closer
     * - Avoids getting stuck on corners
     *
     * @param delta Time since last frame
     */
    private void updateSahurAI(final float delta) {
        // Check line of sight to player
        final boolean canSeePlayer = PathFinder.hasLineOfSight(
            maze, sahurPosition.x, sahurPosition.z,
            playerPosition.x, playerPosition.z, WALL_SIZE
        );

        // Update memory timer and last known position
        if (canSeePlayer) {
            timeSincePlayerSeen = 0;
            lastKnownPlayerPosition.set(playerPosition);
            aiState = AIState.CHASING;
        } else {
            timeSincePlayerSeen += delta;

            // Transition from CHASING to PURSUING when we lose sight
            if (aiState == AIState.CHASING) {
                aiState = AIState.PURSUING;
            }

            // After 10 seconds of not seeing player, give up pursuit
            if (timeSincePlayerSeen > CHASE_MEMORY_DURATION && aiState == AIState.PURSUING) {
                aiState = AIState.WANDERING;
                wanderUpdateTimer = 0;
            }
        }

        Vector3 targetDirection = null;

        // Behavior based on current state
        switch (aiState) {
            case CHASING:
                // Move directly towards player (we can see them)
                targetDirection = new Vector3(
                    playerPosition.x - sahurPosition.x,
                    0,
                    playerPosition.z - sahurPosition.z
                );
                break;

            case PURSUING:
                // Move towards last known player position
                targetDirection = new Vector3(
                    lastKnownPlayerPosition.x - sahurPosition.x,
                    0,
                    lastKnownPlayerPosition.z - sahurPosition.z
                );

                // If we reached last known position, start pathfinding
                if (targetDirection.len() < 2f) {
                    aiState = AIState.PATHFINDING;
                    updatePathToPlayer();
                }
                break;

            case WANDERING:
                // Update wander target periodically
                wanderUpdateTimer += delta;
                if (wanderUpdateTimer >= WANDER_UPDATE_INTERVAL) {
                    wanderUpdateTimer = 0;
                    updateWanderTarget();
                }

                // Move towards wander target
                if (wanderTarget != null) {
                    targetDirection = new Vector3(
                        wanderTarget.x - sahurPosition.x,
                        0,
                        wanderTarget.z - sahurPosition.z
                    );

                    // If reached wander target, pick new one
                    if (targetDirection.len() < 1f) {
                        updateWanderTarget();
                    }
                }

                // Occasionally use pathfinding to get closer to player
                pathUpdateTimer += delta;
                if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
                    pathUpdateTimer = 0;
                    aiState = AIState.PATHFINDING;
                    updatePathToPlayer();
                }
                break;

            case PATHFINDING:
                // Follow A* path
                if (sahurPath != null && !sahurPath.isEmpty() && sahurPathIndex < sahurPath.size()) {
                    final int[] targetGrid = sahurPath.get(sahurPathIndex);
                    final float targetX = targetGrid[0] * WALL_SIZE + WALL_SIZE / 2f;
                    final float targetZ = targetGrid[1] * WALL_SIZE + WALL_SIZE / 2f;

                    targetDirection = new Vector3(targetX - sahurPosition.x, 0, targetZ - sahurPosition.z);

                    if (targetDirection.len() < 1f) {
                        sahurPathIndex++;
                        if (sahurPathIndex >= sahurPath.size()) {
                            // Finished path, check if we should still pursue
                            if (timeSincePlayerSeen < CHASE_MEMORY_DURATION) {
                                aiState = AIState.PURSUING;
                            } else {
                                aiState = AIState.WANDERING;
                            }
                        }
                    }
                } else {
                    // No valid path, check if we should still pursue
                    if (timeSincePlayerSeen < CHASE_MEMORY_DURATION) {
                        aiState = AIState.PURSUING;
                    } else {
                        aiState = AIState.WANDERING;
                    }
                }
                break;
        }

        // Stuck detection - check if we haven't moved much
        stuckTimer += delta;
        if (stuckTimer > STUCK_THRESHOLD) {
            final float distanceMoved = sahurPosition.dst(lastPosition);
            if (distanceMoved < 0.5f) {
                // We're stuck! Try a different approach
                handleStuck();
            }
            lastPosition.set(sahurPosition);
            stuckTimer = 0;
        }

        // Apply movement if we have a target direction
        if (targetDirection != null && targetDirection.len() > 0.1f) {
            // Calculate rotation to face movement direction
            sahurYaw = (float) Math.toDegrees(Math.atan2(targetDirection.x, targetDirection.z));

            // Move towards target with circular collision detection
            targetDirection.nor().scl(SAHUR_SPEED * delta);
            final Vector3 newPosition = sahurPosition.cpy().add(targetDirection);

            // Use circular collision detection for smooth corner navigation
            if (!checkSahurCollision(newPosition)) {
                // No collision, move freely
                sahurPosition.set(newPosition);
            } else {
                // Try sliding along walls (X direction)
                final Vector3 slideX = sahurPosition.cpy().add(targetDirection.x, 0, 0);
                if (!checkSahurCollision(slideX)) {
                    sahurPosition.set(slideX);
                } else {
                    // Try sliding along walls (Z direction)
                    final Vector3 slideZ = sahurPosition.cpy().add(0, 0, targetDirection.z);
                    if (!checkSahurCollision(slideZ)) {
                        sahurPosition.set(slideZ);
                    }
                    // If both fail, Sahur is stuck in a corner and doesn't move
                }
            }
        }

        // Safety check: ensure Sahur hasn't clipped into a wall somehow
        if (checkSahurCollision(sahurPosition)) {
            // Sahur somehow ended up colliding with a wall, revert to last known good position
            sahurPosition.set(lastPosition);
            System.out.println("Warning: Sahur clipped into wall, reverting to last position");
        }

        // Update Sahur's transform
        updateSahurTransform();
    }

    /**
     * Handles when Sahur gets stuck on geometry.
     */
    private void handleStuck() {
        // Force a new wander target or path
        if (aiState == AIState.WANDERING || aiState == AIState.PURSUING) {
            updateWanderTarget();
        } else if (aiState == AIState.PATHFINDING) {
            // Skip to next waypoint or abandon path
            sahurPathIndex++;
            if (sahurPathIndex >= sahurPath.size()) {
                aiState = AIState.WANDERING;
                updateWanderTarget();
            }
        }
    }

    /**
     * Updates the wander target to a random valid adjacent cell.
     */
    private void updateWanderTarget() {
        final int currentGridX = (int) (sahurPosition.x / WALL_SIZE);
        final int currentGridZ = (int) (sahurPosition.z / WALL_SIZE);

        // Validate current position is within bounds
        if (currentGridX < 0 || currentGridX >= maze.getWidth()
            || currentGridZ < 0 || currentGridZ >= maze.getHeight()) {
            // If out of bounds, move to center of maze
            wanderTarget.set(
                maze.getWidth() * WALL_SIZE / 2f,
                SAHUR_HEIGHT,
                maze.getHeight() * WALL_SIZE / 2f
            );
            return;
        }

        // Try to find a valid adjacent cell (including diagonals for more movement options)
        final int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0},
                                     {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        final List<int[]> validDirections = new ArrayList<>();

        for (final int[] dir : directions) {
            final int targetX = currentGridX + dir[0];
            final int targetZ = currentGridZ + dir[1];

            // Check bounds and wall status
            if (targetX >= 0 && targetX < maze.getWidth()
                && targetZ >= 0 && targetZ < maze.getHeight()
                && !maze.isWall(targetX, targetZ)) {
                validDirections.add(new int[]{targetX, targetZ});
            }
        }

        // Pick a random valid direction
        if (!validDirections.isEmpty()) {
            final int[] chosen = validDirections.get(random.nextInt(validDirections.size()));
            wanderTarget.set(
                chosen[0] * WALL_SIZE + WALL_SIZE / 2f,
                SAHUR_HEIGHT,
                chosen[1] * WALL_SIZE + WALL_SIZE / 2f
            );
        } else {
            // No valid neighbors, stay at current position (shouldn't happen often)
            wanderTarget.set(sahurPosition);
            System.out.println("Warning: Sahur has no valid wander targets at ("
                + currentGridX + ", " + currentGridZ + ")");
        }
    }

    /**
     * Updates the A* path to the player's position.
     */
    private void updatePathToPlayer() {
        final int sahurGridX = (int) (sahurPosition.x / WALL_SIZE);
        final int sahurGridZ = (int) (sahurPosition.z / WALL_SIZE);
        final int playerGridX = (int) (playerPosition.x / WALL_SIZE);
        final int playerGridZ = (int) (playerPosition.z / WALL_SIZE);

        // Only pathfind if not at same position
        if (sahurGridX != playerGridX || sahurGridZ != playerGridZ) {
            sahurPath = PathFinder.findPath(maze, sahurGridX, sahurGridZ, playerGridX, playerGridZ);
            sahurPathIndex = 0;
        } else {
            aiState = AIState.WANDERING;
        }
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

    /**
     * Checks if Sahur's position would collide with any walls using circular collision.
     * Uses a wider radius than the player to ensure smooth navigation around corners.
     *
     * @param position Position to check
     * @return True if collision detected
     */
    private boolean checkSahurCollision(final Vector3 position) {
        // Check collision with circle vs AABB (accurate method)
        final int gridX = (int) Math.floor(position.x / WALL_SIZE);
        final int gridZ = (int) Math.floor(position.z / WALL_SIZE);

        // Check 3x3 grid around Sahur's position (wider check due to larger radius)
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
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

                    // Find closest point on wall box to Sahur's circle center
                    final float closestX = Math.max(wallMinX, Math.min(position.x, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(position.z, wallMaxZ));

                    // Calculate distance from Sahur to closest point
                    final float dx2 = position.x - closestX;
                    final float dz2 = position.z - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;

                    // Collision if distance is less than Sahur's collision radius
                    if (distSquared < SAHUR_COLLISION_RADIUS * SAHUR_COLLISION_RADIUS) {
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
        if (materialManager != null) {
            materialManager.dispose();
        }
        if (lightingManager != null) {
            lightingManager.dispose();
        }
    }

    @Override
    public void dispose() {
        hide();
    }
}
