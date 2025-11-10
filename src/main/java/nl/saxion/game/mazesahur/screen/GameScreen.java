package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import nl.saxion.game.mazesahur.config.GameConfig;
import nl.saxion.game.mazesahur.entity.Player;
import nl.saxion.game.mazesahur.entity.Enemy;
import nl.saxion.game.mazesahur.rendering.LightingManager;
import nl.saxion.game.mazesahur.rendering.MaterialManager;
import nl.saxion.game.mazesahur.rendering.MazeRenderer;
import nl.saxion.game.mazesahur.world.Maze;
import nl.saxion.game.mazesahur.ui.GameUI;
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

    // Rendering systems
    private LightingManager lightingManager;
    private MaterialManager materialManager;
    private MazeRenderer mazeRenderer;

    // UI
    private GameUI gameUI;

    // Camera control
    private float yaw;
    private float pitch;
    private int lastMouseX;
    private int lastMouseY;
    private boolean firstMouse;

    private static final float MOUSE_SENSITIVITY = 0.2f;
    private static final float MAX_PITCH = 89f;

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

        // Camera control initialization
        yaw = 0;
        pitch = 0;
        firstMouse = true;
    }

    @Override
    public void show() {
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
    }

    @Override
    public void render(final float delta) {
        // Update game state
        handleInput(delta);
        player.update(delta, maze);
        enemy.update(delta);
        updateCamera();

        // Update lighting
        final boolean isMoving = player.isMoving();
        lightingManager.updateFlashlight(player.getPosition(), camera.direction, delta, isMoving);

        // Clear screen
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render 3D scene
        mazeRenderer.render(camera);
        mazeRenderer.renderEnemy(camera, enemy);

        // Render UI
        gameUI.render(this, player, enemy, lightingManager);

        // Handle input
        handleGameInput();
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

        // Apply movement with collision detection
        if (moveDirection.len() > 0) {
            moveDirection.nor().scl(GameConfig.PLAYER_MOVE_SPEED * delta);

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
     * Checks collision with maze walls using circular collision detection.
     */
    private boolean checkCollision(final Vector3 position) {
        final int gridX = (int) Math.floor(position.x / Maze.CELL_SIZE);
        final int gridZ = (int) Math.floor(position.z / Maze.CELL_SIZE);

        // Check 3x3 grid around player
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

        return false;
    }

    /**
     * Handles non-movement game input (flashlight toggle, exit, etc.).
     */
    private void handleGameInput() {
        // Toggle flashlight
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            lightingManager.toggleFlashlight();
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

        // Calculate look direction
        final double yawRad = Math.toRadians(yaw);
        final double pitchRad = Math.toRadians(pitch);

        final float lookX = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        final float lookY = (float) Math.sin(pitchRad);
        final float lookZ = -(float) (Math.cos(pitchRad) * Math.cos(yawRad));

        camera.direction.set(lookX, lookY, lookZ).nor();
        camera.update();
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
    }

    @Override
    public void dispose() {
        hide();
    }
}

