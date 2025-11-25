package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.entity.Elevator;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.rendering.MaterialManager;
import nl.saxion.game.mazesahur.rendering.MazeRenderer;
import nl.saxion.game.mazesahur.world.Maze;
import nl.saxion.game.mazesahur.ui.GameUI;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Main game screen for the 3D horror maze game.
 * Handles rendering, player input, enemy AI, and game logic.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class GameScreen extends ScalableGameScreen {

    // Core components
    private PerspectiveCamera camera;
    private final Player player;
    private final Enemy enemy;
    private final Maze maze;
    private Elevator elevator;

    // Rendering systems
    private LightingManager lightingManager;
    private MaterialManager materialManager;
    private MazeRenderer mazeRenderer;

    // UI
    private GameUI gameUI;

    // Audio
    private Sound flashlightToggleSound;

    // Camera control
    private float yaw;
    private float pitch;
    private int lastMouseX;
    private int lastMouseY;
    private boolean firstMouse;

    private static final float MOUSE_SENSITIVITY = 0.2f;
    private static final float MAX_PITCH = 89f;

    // Initialization flag to prevent double-loading
    private boolean initialized = false;

    // Death and jumpscare state
    private boolean isDead = false;
    private float jumpscareTimer = 0f;
    private float survivalTime = 0f;
    private Vector3 jumpscareShakeOffset = new Vector3();
    private boolean jumpscareActive = false;

    // Debug visualization
    private boolean showRailNetwork = false;

    /**
     * Creates a new game screen with default settings.
     */
    public GameScreen() {
        super(1280, 720);

        // Initialize world
        maze = new Maze(25, 25);
        maze.generate();

        // Initialize entities
        player = new Player(new Vector3(12f, 3f, 12f));
        enemy = new Enemy(maze, player);

        // Initialize elevator in a valid open position
        elevator = createElevatorInOpenSpace();

        // Camera control initialization
        yaw = 0;
        pitch = 0;
        firstMouse = true;
    }

    @Override
    public void show() {
        // Prevent double-initialization (happens when switching from splash)
        if (initialized) {
            System.out.println("[GameScreen] Already initialized, updating viewport...");

            // Update camera viewport for new window size (900x500 -> 1280x720)
            final int screenWidth = Gdx.graphics.getBackBufferWidth();
            final int screenHeight = Gdx.graphics.getBackBufferHeight();
            if (camera != null) {
                camera.viewportWidth = screenWidth;
                camera.viewportHeight = screenHeight;
                camera.update();
            }

            // Recapture cursor
            Gdx.input.setCursorCatched(true);
            return;
        }

        System.out.println("[GameScreen] Initializing for the first time...");
        initialized = true;

        // Initialize camera (must be done after OpenGL context is ready)
        final int screenWidth = Gdx.graphics.getBackBufferWidth();
        final int screenHeight = Gdx.graphics.getBackBufferHeight();
        camera = new PerspectiveCamera(67, screenWidth, screenHeight);
        camera.near = 0.01f;
        camera.far = 100f;

        // Initialize rendering systems (requires OpenGL context)
        lightingManager = new LightingManager();
        materialManager = new MaterialManager();
        mazeRenderer = new MazeRenderer(maze, materialManager, lightingManager);

        // Initialize UI
        gameUI = new GameUI();
        gameUI.initialize();

        // Load audio
        flashlightToggleSound = Gdx.audio.newSound(Gdx.files.internal("audio/light-switch-81967.mp3"));

        // Capture cursor for FPS controls
        Gdx.input.setCursorCatched(true);

        // Update camera
        camera.position.set(player.getPosition());
        camera.lookAt(player.getPosition().x, player.getPosition().y, player.getPosition().z - 1);
        camera.update();

        // Initialize rendering systems
        materialManager.loadTextures();
        mazeRenderer.initialize();

        // Initialize enemy position
        enemy.initialize();

        System.out.println("[GameScreen] Initialization complete!");
    }

    @Override
    public void render(final float delta) {
        // Handle jumpscare sequence
        if (jumpscareActive) {
            handleJumpscare(delta);
            return;
        }

        // Check for death condition
        if (!isDead) {
            survivalTime += delta;
            // TEMPORARY: Death disabled for testing
            // checkDeathCondition();
        }

        // Skip normal game logic if dead
        if (!isDead) {
            // Update game state
            handleInput(delta);
            player.update(delta, maze);
            enemy.update(delta);
            elevator.update(delta, player.getPosition()); // Update elevator with player position
            updateCamera();

            // Update lighting
            final boolean isMoving = player.isMoving();
            lightingManager.updateFlashlight(player.getPosition(), camera.direction, delta, isMoving);
            mazeRenderer.updateLampFlicker(delta);

            // Handle input
            handleGameInput();
        }

        // Clear screen
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render 3D scene (elevator is rendered together with maze to prevent material bleeding)
        mazeRenderer.renderWithElevator(camera, elevator);
        mazeRenderer.renderEnemy(camera, enemy);

        // Render debug visualizations
        if (showRailNetwork) {
            mazeRenderer.renderRailNetworkDebug(camera, enemy.getRailNetwork());
        }

        // Render UI (hide during jumpscare)
        if (!jumpscareActive) {
            gameUI.render(this, player, enemy, elevator, lightingManager);
        }
    }

    /**
     * Handles mouse look and WASD movement input.
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
        if (pitch > MAX_PITCH) {
            pitch = MAX_PITCH;
        }
        if (pitch < -MAX_PITCH) {
            pitch = -MAX_PITCH;
        }

        // Calculate movement direction
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

        // Check if player is trying to run (Shift key)
        final boolean tryingToRun = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                                     || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        // Can only run if moving and has energy
        final boolean canRun = moveDirection.len() > 0 && tryingToRun && player.getEnergy() > 0.0f;
        player.setRunning(canRun);

        // Drain energy while running
        if (canRun) {
            player.drainEnergy(delta);
        }

        // Apply movement with collision detection
        if (moveDirection.len() > 0) {
            // Calculate speed based on energy level
            final float speedMultiplier = player.getCurrentSpeedMultiplier();
            final float currentSpeed = GameConfig.PLAYER_MOVE_SPEED * speedMultiplier;

            moveDirection.nor().scl(currentSpeed * delta);

            // Try full movement first
            final Vector3 newPosition = player.getPosition().cpy().add(moveDirection);

            if (!checkCollision(newPosition)) {
                // No collision, move freely
                player.getPosition().set(newPosition);
            } else {
                // Try sliding along walls (X direction only)
                final Vector3 slideX = player.getPosition().cpy().add(moveDirection.x, 0, 0);
                if (!checkCollision(slideX)) {
                    player.getPosition().set(slideX);
                } else {
                    // Try sliding along walls (Z direction only)
                    final Vector3 slideZ = player.getPosition().cpy().add(0, 0, moveDirection.z);
                    if (!checkCollision(slideZ)) {
                        player.getPosition().set(slideZ);
                    }
                    // If both fail, player is stuck in corner and doesn't move
                }
            }
        }
    }

    /**
     * Creates an elevator using the maze's guaranteed position finder.
     * The maze will find a suitable wall and prepare it for the elevator.
     */
    private Elevator createElevatorInOpenSpace() {
        // Player spawns at (12, 3, 12) world coordinates
        final float playerX = 12f;
        final float playerZ = 12f;

        final int playerGridX = (int) Math.floor(playerX / Maze.CELL_SIZE);
        final int playerGridZ = (int) Math.floor(playerZ / Maze.CELL_SIZE);

        // Let the maze find and prepare a guaranteed elevator position
        final int[] elevatorInfo = maze.findAndPrepareElevatorPosition(playerGridX, playerGridZ);

        // Extract position info
        final int wallX = elevatorInfo[0];
        final int wallZ = elevatorInfo[1];
        final int openX = elevatorInfo[2];
        final int openZ = elevatorInfo[3];
        final int direction = elevatorInfo[4];

        // Convert wall position to world coordinates
        final float[] wallWorldPos = maze.gridToWorld(wallX, wallZ);
        final float elevatorX = wallWorldPos[0];
        final float elevatorZ = wallWorldPos[1];

        // Direction names for debug
        final String[] dirNames = {"North (wall is North)", "East (wall is East)",
                                   "South (wall is South)", "West (wall is West)"};

        System.out.println("[GameScreen] ===== ELEVATOR SPAWN =====");
        System.out.println("[GameScreen] Player spawn: (" + playerX + ", " + playerZ + ")");
        System.out.println("[GameScreen] Elevator grid (IN WALL): (" + wallX + ", " + wallZ + ")");
        System.out.println("[GameScreen] Elevator world: (" + elevatorX + ", " + elevatorZ + ")");
        System.out.println("[GameScreen] Open space grid: (" + openX + ", " + openZ + ")");
        System.out.println("[GameScreen] Door faces: " + dirNames[direction]);
        System.out.println("[GameScreen] ===========================");

        return new Elevator(maze, elevatorX, elevatorZ);
    }

    /**
     * Checks collision at the specified world position.
     * Checks collision with maze walls and elevator using circular collision detection.
     */
    private boolean checkCollision(final Vector3 position) {
        final int gridX = (int) Math.floor(position.x / Maze.CELL_SIZE);
        final int gridZ = (int) Math.floor(position.z / Maze.CELL_SIZE);

        // Check 3x3 grid around player for walls
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                final int checkX = gridX + dx;
                final int checkZ = gridZ + dz;

                if (checkX >= 0 && checkX < maze.getWidth()
                    && checkZ >= 0 && checkZ < maze.getHeight()
                    && maze.isWall(checkX, checkZ)) {

                    // Wall bounds in world space
                    final float wallMinX = checkX * Maze.CELL_SIZE;
                    final float wallMaxX = wallMinX + Maze.CELL_SIZE;
                    final float wallMinZ = checkZ * Maze.CELL_SIZE;
                    final float wallMaxZ = wallMinZ + Maze.CELL_SIZE;

                    // Find closest point on wall box to player circle center
                    final float closestX = Math.max(wallMinX, Math.min(position.x, wallMaxX));
                    final float closestZ = Math.max(wallMinZ, Math.min(position.z, wallMaxZ));

                    // Calculate distance from player to closest point
                    final float dx2 = position.x - closestX;
                    final float dz2 = position.z - closestZ;
                    final float distSquared = dx2 * dx2 + dz2 * dz2;

                    // Collision if distance is less than collision radius
                    if (distSquared < GameConfig.PLAYER_COLLISION_RADIUS * GameConfig.PLAYER_COLLISION_RADIUS) {
                        return true;
                    }
                }
            }
        }

        // Check collision with elevator (only blocks if doors are closed)
        if (elevator.blocksPosition(position)) {
            return true;
        }

        // TODO: Re-enable door frame collision after testing spawn position
        // Check collision with elevator door frame (walls beside the door)
        // if (elevator.collidesWithDoorFrame(position, GameConfig.PLAYER_COLLISION_RADIUS)) {
        //     return true;
        // }

        return false;
    }

    /**
     * Handles non-movement game input (flashlight toggle, exit, elevator control, etc.).
     */
    private void handleGameInput() {
        // Toggle flashlight
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            lightingManager.toggleFlashlight();
            // Play light switch sound
            if (flashlightToggleSound != null) {
                flashlightToggleSound.play(0.7f); // Volume at 70%
            }
        }

        // Toggle rail network visualization (debug)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            showRailNetwork = !showRailNetwork;
            System.out.println("[GameScreen] Rail network visualization: "
                + (showRailNetwork ? "ON" : "OFF"));
        }

        // Toggle elevator doors with E key
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (elevator != null) {
                // Check if player is close enough to the elevator
                final float distanceToElevator = elevator.getDistanceToPlayer(player.getPosition());
                if (distanceToElevator <= 8.0f) { // Within 8 units
                    elevator.toggleDoors();
                } else {
                    System.out.println("[GameScreen] Too far from elevator to control doors (distance: " + distanceToElevator + ")");
                }
            }
        }

        // Exit game
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
            Gdx.app.exit();
        }
    }

    /**
     * Gets the forward direction vector based on yaw.
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
        camera.position.set(player.getPosition());

        // Apply jumpscare screen shake if active
        if (jumpscareActive) {
            camera.position.add(jumpscareShakeOffset);
        }

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
     * Checks if the player has been caught by the enemy.
     */
    private void checkDeathCondition() {
        // Calculate horizontal distance only (ignore Y-axis height difference)
        final Vector3 playerPos = player.getPosition();
        final Vector3 enemyPos = enemy.getPosition();

        final float dx = playerPos.x - enemyPos.x;
        final float dz = playerPos.z - enemyPos.z;
        final float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance <= enemy.getCatchRadius()) {
            triggerDeath();
        }
    }

    /**
     * Triggers the death sequence with jumpscare effects.
     */
    private void triggerDeath() {
        if (isDead) return; // Already dead

        isDead = true;
        jumpscareActive = true;
        jumpscareTimer = 0f;

        System.out.println("[GameScreen] Player caught! Triggering jumpscare...");

        // Snap camera to face enemy
        final Vector3 toEnemy = enemy.getPosition().cpy().sub(player.getPosition());
        final float angleToEnemy = (float) Math.toDegrees(Math.atan2(toEnemy.x, -toEnemy.z));
        yaw = angleToEnemy;
        pitch = 0f; // Level camera
    }

    /**
     * Handles the jumpscare animation sequence.
     * Sequence: Red flash -> Screen shake -> Fade to black -> Death screen
     */
    private void handleJumpscare(final float delta) {
        jumpscareTimer += delta;

        final float progress = jumpscareTimer / GameConfig.JUMPSCARE_DURATION;

        // Phase 1 (0-0.3s): Red flash (quick fade out)
        if (jumpscareTimer < 0.3f) {
            final float flashAlpha = 1.0f - (jumpscareTimer / 0.3f);
            Gdx.gl.glClearColor(flashAlpha, 0f, 0f, 1f);
        }

        // Phase 2 (0-0.5s): Violent screen shake
        if (jumpscareTimer < 0.5f) {
            final float shakeIntensity = GameConfig.JUMPSCARE_SHAKE_INTENSITY * (1.0f - jumpscareTimer / 0.5f);
            jumpscareShakeOffset.set(
                (float) (Math.random() - 0.5) * shakeIntensity * 2,
                (float) (Math.random() - 0.5) * shakeIntensity * 2,
                (float) (Math.random() - 0.5) * shakeIntensity * 2
            );
        } else {
            jumpscareShakeOffset.set(0, 0, 0);
        }

        // Update camera to apply shake
        updateCamera();

        // Clear and render scene
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render(camera);
        mazeRenderer.renderEnemy(camera, enemy);

        // Phase 3 (0.5s-1.5s): Fade to black overlay
        if (jumpscareTimer > 0.5f) {
            // Draw fading black overlay (simplified - would need SpriteBatch in real implementation)
            final float fadeAlpha = Math.min(1.0f, (jumpscareTimer - 0.5f) / 1.0f);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glClearColor(0f, 0f, 0f, fadeAlpha);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Phase 4 (1.5s+): Transition to death screen
        if (jumpscareTimer >= GameConfig.JUMPSCARE_DURATION) {
            transitionToDeathScreen();
        }
    }

    /**
     * Transitions from the game screen to the death screen.
     */
    private void transitionToDeathScreen() {
        System.out.println("[GameScreen] Transitioning to death screen...");

        final DeathScreen deathScreen = new DeathScreen(survivalTime);

        // Initialize the death screen
        deathScreen.show();

        // Register and switch to death screen (GameApp handles disposing old screen)
        GameApp.addScreen("DeathScreen", deathScreen);
        GameApp.switchScreen("DeathScreen");
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
        gameUI.dispose();
        mazeRenderer.dispose();
        materialManager.dispose();
        lightingManager.dispose();
        if (flashlightToggleSound != null) {
            flashlightToggleSound.dispose();
        }
    }

    @Override
    public void dispose() {
        hide();
    }
}

