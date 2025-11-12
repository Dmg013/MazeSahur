package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.graphics.Color;

/**
 * Defines different surface types with their material properties.
 * Each surface type has predefined PBR-like properties for realistic rendering.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public enum SurfaceType {

    /**
     * Red brick wall with plaster patches - rough, minimal reflectivity.
     */
    WALL(
        "textures/wall/",
        "red_brick_plaster_patch_02",
        "4k",
        new Color(0.8f, 0.8f, 0.8f, 1f),      // Diffuse tint (slightly brighter for lighting)
        new Color(0.03f, 0.03f, 0.03f, 1f),   // Specular (very low - rough brick)
        new Color(0.4f, 0.4f, 0.4f, 1f),      // Ambient
        1.5f                                   // Shininess (very rough surface)
    ),

    /**
     * Concrete floor - rough with slight dampness.
     */
    FLOOR(
        "textures/floor/",
        "concrete_floor_damaged_01",
        "4k",
        new Color(0.85f, 0.85f, 0.88f, 1f),   // Diffuse tint (cool concrete tone)
        new Color(0.08f, 0.08f, 0.08f, 1f),   // Specular (slight sheen from dampness)
        new Color(0.6f, 0.6f, 0.65f, 1f),     // Ambient
        2.5f                                   // Shininess (slightly smoother than wall)
    ),

    /**
     * Concrete layers roof - layered concrete ceiling with reflective surface.
     */
    ROOF(
        "textures/roof/",
        "concrete_layers",
        "4k",
        new Color(0.85f, 0.85f, 0.88f, 1f),   // Diffuse tint (brighter for reflection)
        new Color(0.6f, 0.6f, 0.65f, 1f),     // Specular (high reflection)
        new Color(0.5f, 0.5f, 0.55f, 1f),     // Ambient (darker for ceiling)
        64.0f                                  // Shininess (reflective surface)
    );

    private final String basePath;
    private final String textureName;
    private final String resolution;
    private final Color diffuseTint;
    private final Color specularColor;
    private final Color ambientColor;
    private final float shininess;

    /**
     * Creates a surface type with specified properties.
     *
     * @param basePath Base texture directory
     * @param textureName Texture name without suffixes
     * @param resolution Resolution suffix
     * @param diffuseTint Color tint for diffuse
     * @param specularColor Specular reflection color
     * @param ambientColor Ambient light color
     * @param shininess Shininess/roughness value
     */
    SurfaceType(final String basePath, final String textureName, final String resolution,
                final Color diffuseTint, final Color specularColor, final Color ambientColor,
                final float shininess) {
        this.basePath = basePath;
        this.textureName = textureName;
        this.resolution = resolution;
        this.diffuseTint = diffuseTint;
        this.specularColor = specularColor;
        this.ambientColor = ambientColor;
        this.shininess = shininess;
    }

    /**
     * Gets the base texture directory path.
     *
     * @return Base path
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Gets the texture name without suffixes.
     *
     * @return Texture name
     */
    public String getTextureName() {
        return textureName;
    }

    /**
     * Gets the resolution suffix.
     *
     * @return Resolution string
     */
    public String getResolution() {
        return resolution;
    }

    /**
     * Gets the diffuse color tint.
     *
     * @return Diffuse tint color
     */
    public Color getDiffuseTint() {
        return new Color(diffuseTint);
    }

    /**
     * Gets the specular reflection color.
     *
     * @return Specular color
     */
    public Color getSpecularColor() {
        return new Color(specularColor);
    }

    /**
     * Gets the ambient light color.
     *
     * @return Ambient color
     */
    public Color getAmbientColor() {
        return new Color(ambientColor);
    }

    /**
     * Gets the shininess/roughness value.
     *
     * @return Shininess value
     */
    public float getShininess() {
        return shininess;
    }

    /**
     * Creates a texture set for this surface type.
     *
     * @return New texture set instance
     */
    public TextureSet createTextureSet() {
        return new TextureSet(basePath, textureName, resolution);
    }
}
