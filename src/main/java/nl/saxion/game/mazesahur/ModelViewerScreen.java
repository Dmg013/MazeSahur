package nl.saxion.game.mazesahur;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Screen that displays a 3D model loaded from an OBJ file.
 * The model rotates continuously for demonstration.
 */
public class ModelViewerScreen extends ScalableGameScreen {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private ModelInstance modelInstance;
    private Environment environment;
    private float rotationAngle = 0f;
    private static final float ROTATION_SPEED = 30f; // degrees per second
    private static final float MAX_ROTATION_ANGLE = 360f;

    /**
     * Constructs a new ModelViewerScreen.
     */
    public ModelViewerScreen() {
        super(1280, 720);
    }

    /**
     * Called when this screen becomes active.
     * Loads the 3D model and sets up the camera and lighting.
     */
    @Override
    public void show() {
        // Load UI font
        GameApp.addFont("ui", "fonts/basic.ttf", 24);

        // Setup 3D camera - use FULL screen dimensions
        final int screenWidth = Gdx.graphics.getBackBufferWidth();
        final int screenHeight = Gdx.graphics.getBackBufferHeight();
        camera = new PerspectiveCamera(67, screenWidth, screenHeight);
        camera.position.set(1.5f, 1.5f, 1.5f);  // Much closer for tiny model
        camera.lookAt(0, 0.5f, 0);  // Look at center height
        camera.near = 0.001f;
        camera.far = 200f;
        camera.update();

        // Setup ModelBatch for 3D rendering
        modelBatch = new ModelBatch();

        // Load OBJ model with flipped V coordinates (common for different modeling software)
        ObjLoader objLoader = new ObjLoader();
        ObjLoader.ObjLoaderParameters params = new ObjLoader.ObjLoaderParameters();
        params.flipV = true;  // Flip vertical texture coordinates
        model = objLoader.loadModel(Gdx.files.internal("models/tung tung tung sahur.obj"), params);

        // Create model instance
        modelInstance = new ModelInstance(model);

        // Manually load and apply texture with mipmapping for better quality
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(
            Gdx.files.internal("models/Material.png"),
            true  // Generate mipmaps for better quality at different distances
        );
        // Use MipMapLinearLinear for best quality (trilinear filtering)
        texture.setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );
        // Set texture wrapping to ClampToEdge to prevent repeating/stretching
        texture.setWrap(
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge,
            com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
        );

        // Apply texture to all materials in the model
        for (com.badlogic.gdx.graphics.g3d.Material material : modelInstance.materials) {
            material.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(texture));
        }

        // Calculate bounding box to determine model size
        modelInstance.calculateBoundingBox(new com.badlogic.gdx.math.collision.BoundingBox());

        // Scale down the model SUPER TINY - 5x smaller again
        final float modelScale = 0.001f;  // Even tinier
        modelInstance.transform.setToScaling(modelScale, modelScale, modelScale);

        // Setup lighting environment - brighter lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(1.0f, 1.0f, 1.0f, -1f, -0.8f, -0.2f));
    }

    /**
     * Renders the 3D model with rotation animation.
     *
     * @param delta Time in seconds since the last frame
     */
    @Override
    public void render(final float delta) {
        // Update rotation
        rotationAngle += ROTATION_SPEED * delta;
        if (rotationAngle >= MAX_ROTATION_ANGLE) {
            rotationAngle -= MAX_ROTATION_ANGLE;
        }

        // Apply rotation to model (rotate around Y axis) while keeping scale
        final float modelScale = 0.001f;  // Same super tiny scale as in show()
        final float xAxis = 0;
        final float yAxis = 1;
        final float zAxis = 0;
        modelInstance.transform.setToScaling(modelScale, modelScale, modelScale);
        modelInstance.transform.rotate(xAxis, yAxis, zAxis, rotationAngle);

        // Clear screen - use FULL screen size
        final int screenWidth = Gdx.graphics.getBackBufferWidth();
        final int screenHeight = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1f);

        // Render 3D model
        modelBatch.begin(camera);
        modelBatch.render(modelInstance, environment);
        modelBatch.end();

        // Draw 2D UI overlay
        GameApp.startSpriteRendering();
        GameApp.drawText("ui", "MazeSahur - 3D Model Viewer", 20, getWorldHeight() - 20, "white");
        GameApp.drawText("ui", "Model: Tung Tung Tung Sahur", 20, getWorldHeight() - 50, "white");
        GameApp.drawText("ui", "Press ESC to exit", 20, 30, "amber-500");
        GameApp.endSpriteRendering();

        // Handle input
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    /**
     * Called when the window is resized.
     *
     * @param width New window width
     * @param height New window height
     */
    @Override
    public void resize(final int width, final int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    /**
     * Called when this screen is no longer active.
     * Cleans up resources.
     */
    @Override
    public void hide() {
        GameApp.disposeFont("ui");
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        if (model != null) {
            model.dispose();
        }
    }

    /**
     * Disposes of all resources.
     */
    @Override
    public void dispose() {
        hide();
    }
}
