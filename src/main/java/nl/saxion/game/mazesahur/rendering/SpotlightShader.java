package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Custom shader for realistic spotlight rendering.
 * Implements proper cone-based spotlight with distance attenuation and smooth falloff.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class SpotlightShader implements Shader {
    private ShaderProgram program;
    private Camera camera;
    private RenderContext context;

    // Spotlight parameters
    private final Vector3 spotPosition = new Vector3();
    private final Vector3 spotDirection = new Vector3(0, 0, -1);
    private final Vector3 spotColor = new Vector3(1.4f, 1.4f, 1.35f);
    private float spotIntensity = 1.5f;
    private float spotCutoff = (float) Math.cos(Math.toRadians(25.0)); // Inner cone angle
    private float spotOuterCutoff = (float) Math.cos(Math.toRadians(35.0)); // Outer cone angle
    private float spotAttenuation = 0.015f;
    private boolean enabled = true;

    // Ambient and moonlight
    private final Vector3 ambientLight = new Vector3(0.01f, 0.01f, 0.012f);
    private final Vector3 moonDirection = new Vector3(-0.2f, -1f, -0.3f);
    private final Vector3 moonColor = new Vector3(0.03f, 0.035f, 0.05f);

    // Fog parameters for horror atmosphere
    private final Vector3 fogColor = new Vector3(0.0f, 0.0f, 0.0f); // Pure black fog
    private float fogDensity = 0.12f;     // How thick the fog is
    private float fogStart = 8.0f;        // Distance where fog starts (meters)
    private float fogEnd = 35.0f;         // Distance where fog is maximum

    // Parallax occlusion mapping parameters
    private float heightScale = 0.06f;    // Depth scale for parallax effect (conservative but visible)

    public SpotlightShader() {
        final String vertexShader = Gdx.files.internal("shaders/spotlight.vertex.glsl").readString();
        final String fragmentShader = Gdx.files.internal("shaders/spotlight.fragment.glsl").readString();

        program = new ShaderProgram(vertexShader, fragmentShader);

        if (!program.isCompiled()) {
            throw new GdxRuntimeException("Shader compilation failed:\n" + program.getLog());
        }
    }

    @Override
    public void init() {
        // Shader is initialized in constructor
    }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        program.bind();

        // Set camera uniforms
        program.setUniformMatrix("u_projViewTrans", camera.combined);
        program.setUniformf("u_cameraPosition", camera.position);

        // Set ambient and moonlight
        program.setUniformf("u_ambientLight", ambientLight);
        program.setUniformf("u_moonDirection", moonDirection.nor());
        program.setUniformf("u_moonColor", moonColor);

        // Set fog parameters
        program.setUniformf("u_fogColor", fogColor);
        program.setUniformf("u_fogDensity", fogDensity);
        program.setUniformf("u_fogStart", fogStart);
        program.setUniformf("u_fogEnd", fogEnd);

        // Set parallax occlusion mapping parameters
        program.setUniformf("u_heightScale", heightScale);

        // Set spotlight parameters
        if (enabled) {
            program.setUniformf("u_spotPosition", spotPosition);
            program.setUniformf("u_spotDirection", spotDirection.nor());
            program.setUniformf("u_spotColor", spotColor);
            program.setUniformf("u_spotIntensity", spotIntensity);
            program.setUniformf("u_spotCutoff", spotCutoff);
            program.setUniformf("u_spotOuterCutoff", spotOuterCutoff);
            program.setUniformf("u_spotAttenuation", spotAttenuation);
        } else {
            // Disable spotlight by setting intensity to 0
            program.setUniformf("u_spotIntensity", 0f);
        }

        context.setDepthTest(GL20.GL_LEQUAL);
        context.setCullFace(GL20.GL_BACK);
    }

    @Override
    public void render(Renderable renderable) {
        // Set per-object uniforms
        program.setUniformMatrix("u_worldTrans", renderable.worldTransform);

        // Extract 3x3 normal matrix from 4x4 world transform
        // Normal matrix is the transpose of the inverse of the upper-left 3x3 of world matrix
        final com.badlogic.gdx.math.Matrix4 worldMat = renderable.worldTransform;
        final com.badlogic.gdx.math.Matrix3 normalMatrix = new com.badlogic.gdx.math.Matrix3();

        // Extract 3x3 rotation/scale part from 4x4 matrix
        final float[] vals = worldMat.val;
        normalMatrix.set(new float[] {
            vals[com.badlogic.gdx.math.Matrix4.M00], vals[com.badlogic.gdx.math.Matrix4.M01], vals[com.badlogic.gdx.math.Matrix4.M02],
            vals[com.badlogic.gdx.math.Matrix4.M10], vals[com.badlogic.gdx.math.Matrix4.M11], vals[com.badlogic.gdx.math.Matrix4.M12],
            vals[com.badlogic.gdx.math.Matrix4.M20], vals[com.badlogic.gdx.math.Matrix4.M21], vals[com.badlogic.gdx.math.Matrix4.M22]
        });

        program.setUniformMatrix("u_normalMatrix", normalMatrix);

        // Bind textures with proper fallbacks
        final com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute diffuseAttr =
            renderable.material.get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.class,
                com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);

        final com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute normalAttr =
            renderable.material.get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.class,
                com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Normal);

        final com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute heightAttr =
            renderable.material.get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.class,
                com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Bump);

        // Bind diffuse texture
        if (diffuseAttr != null && diffuseAttr.textureDescription.texture != null) {
            final int unit = context.textureBinder.bind(diffuseAttr.textureDescription.texture);
            program.setUniformi("u_diffuseTexture", unit);
        }

        // Bind normal map (use diffuse as fallback)
        if (normalAttr != null && normalAttr.textureDescription.texture != null) {
            final int unit = context.textureBinder.bind(normalAttr.textureDescription.texture);
            program.setUniformi("u_normalTexture", unit);
        } else if (diffuseAttr != null && diffuseAttr.textureDescription.texture != null) {
            final int unit = context.textureBinder.bind(diffuseAttr.textureDescription.texture);
            program.setUniformi("u_normalTexture", unit);
        }

        // Bind height map and set flag
        if (heightAttr != null && heightAttr.textureDescription.texture != null) {
            final int unit = context.textureBinder.bind(heightAttr.textureDescription.texture);
            program.setUniformi("u_heightTexture", unit);
            program.setUniformf("u_hasHeightMap", 1.0f);
        } else {
            // Disable parallax occlusion mapping
            program.setUniformf("u_hasHeightMap", 0.0f);
            // Still bind a texture to prevent shader errors (use diffuse)
            if (diffuseAttr != null && diffuseAttr.textureDescription.texture != null) {
                final int unit = context.textureBinder.bind(diffuseAttr.textureDescription.texture);
                program.setUniformi("u_heightTexture", unit);
            }
        }

        // Render
        renderable.meshPart.render(program);
    }

    @Override
    public void end() {
        // Cleanup if needed
    }

    @Override
    public void dispose() {
        if (program != null) {
            program.dispose();
        }
    }

    /**
     * Updates the flashlight position and direction.
     */
    public void updateFlashlight(Vector3 position, Vector3 direction, float intensity) {
        this.spotPosition.set(position);
        this.spotDirection.set(direction).nor();
        this.spotIntensity = intensity;
    }

    /**
     * Sets whether the flashlight is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets whether the flashlight is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets fog density (higher = thicker fog).
     * @param density Fog density (typically 0.05 - 0.2)
     */
    public void setFogDensity(float density) {
        this.fogDensity = density;
    }

    /**
     * Sets fog distance range.
     * @param start Distance where fog begins
     * @param end Distance where fog is maximum
     */
    public void setFogRange(float start, float end) {
        this.fogStart = start;
        this.fogEnd = end;
    }

    /**
     * Sets the height scale for parallax occlusion mapping.
     * Higher values = more pronounced depth effect.
     *
     * @param scale Height scale (recommended: 0.05 - 0.15)
     */
    public void setHeightScale(float scale) {
        this.heightScale = scale;
    }

    /**
     * Gets the current height scale for parallax occlusion mapping.
     *
     * @return Height scale value
     */
    public float getHeightScale() {
        return heightScale;
    }
}

