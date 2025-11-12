package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/**
 * Represents a complete set of PBR texture maps for a surface.
 * Manages loading, filtering, and disposal of all texture components.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class TextureSet {

    private final String basePath;
    private final String textureName;
    private final String resolution;

    // PBR Texture maps
    private Texture diffuseMap;
    private Texture normalMap;
    private Texture aoMap;
    private Texture roughnessMap;
    private Texture displacementMap;
    private Texture specularMap;

    /**
     * Creates a new texture set with the specified parameters.
     *
     * @param basePath Base directory path (e.g., "textures/wall/")
     * @param textureName Texture name without suffixes (e.g., "red_brick_plaster_patch_02")
     * @param resolution Resolution suffix (e.g., "8k")
     */
    public TextureSet(final String basePath, final String textureName, final String resolution) {
        this.basePath = basePath;
        this.textureName = textureName;
        this.resolution = resolution;
    }

    /**
     * Loads all available texture maps with proper filtering and mipmaps.
     * Silently skips maps that don't exist.
     */
    public void loadTextures() {
        diffuseMap = loadTextureIfExists(buildPath("diff"));
        normalMap = loadTextureIfExists(buildPath("nor_gl"));
        aoMap = loadTextureIfExists(buildPath("ao"));
        roughnessMap = loadTextureIfExists(buildPath("rough"));
        displacementMap = loadTextureIfExists(buildPath("disp"));
        specularMap = loadTextureIfExists(buildPath("spec"));
    }

    /**
     * Builds the full path for a texture map type, trying .png, .jpg, and .exr extensions.
     *
     * @param mapType Map type suffix (e.g., "diff", "nor_gl", "ao")
     * @return Full internal file path with working extension, or null if not found
     */
    private String buildPath(final String mapType) {
        final String baseName = basePath + textureName + "_" + mapType + "_" + resolution;

        // Try .png first (newer textures)
        String pngPath = baseName + ".png";
        if (Gdx.files.internal(pngPath).exists()) {
            return pngPath;
        }

        // Try .jpg as fallback (older textures)
        String jpgPath = baseName + ".jpg";
        if (Gdx.files.internal(jpgPath).exists()) {
            return jpgPath;
        }

        // Try .exr for high dynamic range textures
        String exrPath = baseName + ".exr";
        if (Gdx.files.internal(exrPath).exists()) {
            return exrPath;
        }

        // Not found
        return null;
    }

    /**
     * Loads a texture if it exists, with proper filtering and wrapping.
     *
     * @param path Internal file path (can be null)
     * @return Loaded texture or null if file doesn't exist
     */
    private Texture loadTextureIfExists(final String path) {
        if (path == null) {
            return null;
        }

        try {
            if (Gdx.files.internal(path).exists()) {
                final Texture texture = new Texture(Gdx.files.internal(path), true);
                texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                Gdx.app.log("TextureSet", "Loaded: " + path);
                return texture;
            }
        } catch (final Exception e) {
            Gdx.app.error("TextureSet", "Failed to load texture: " + path, e);
        }
        return null;
    }

    /**
     * Gets the diffuse (color) map.
     *
     * @return Diffuse texture or null if not loaded
     */
    public Texture getDiffuseMap() {
        return diffuseMap;
    }

    /**
     * Gets the normal map for surface detail.
     *
     * @return Normal map texture or null if not loaded
     */
    public Texture getNormalMap() {
        return normalMap;
    }

    /**
     * Gets the ambient occlusion map for shadow detail.
     *
     * @return AO map texture or null if not loaded
     */
    public Texture getAoMap() {
        return aoMap;
    }

    /**
     * Gets the roughness map for specular control.
     *
     * @return Roughness map texture or null if not loaded
     */
    public Texture getRoughnessMap() {
        return roughnessMap;
    }

    /**
     * Gets the displacement/height map for parallax effects.
     *
     * @return Displacement map texture or null if not loaded
     */
    public Texture getDisplacementMap() {
        return displacementMap;
    }

    /**
     * Gets the specular map for reflection detail.
     *
     * @return Specular map texture or null if not loaded
     */
    public Texture getSpecularMap() {
        return specularMap;
    }

    /**
     * Checks if this texture set has all essential maps loaded.
     *
     * @return True if at least the diffuse map is available
     */
    public boolean isValid() {
        return diffuseMap != null;
    }

    /**
     * Disposes of all loaded textures.
     */
    public void dispose() {
        if (diffuseMap != null) diffuseMap.dispose();
        if (normalMap != null) normalMap.dispose();
        if (aoMap != null) aoMap.dispose();
        if (roughnessMap != null) roughnessMap.dispose();
        if (displacementMap != null) displacementMap.dispose();
        if (specularMap != null) specularMap.dispose();
    }
}
