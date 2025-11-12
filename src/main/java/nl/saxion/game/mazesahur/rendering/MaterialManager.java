package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages material creation and texture loading for realistic PBR-like rendering.
 * Uses TextureSet and SurfaceType for organized, OOP-based texture management.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 2.0
 */
public class MaterialManager {

    // Texture sets organized by surface type
    private final Map<SurfaceType, TextureSet> textureSets;

    /**
     * Creates a new material manager.
     */
    public MaterialManager() {
        this.textureSets = new EnumMap<>(SurfaceType.class);
    }

    /**
     * Loads all textures with proper filtering and wrapping.
     */
    public void loadTextures() {
        // Load texture sets for each surface type
        for (final SurfaceType surfaceType : SurfaceType.values()) {
            final TextureSet textureSet = surfaceType.createTextureSet();
            textureSet.loadTextures();

            if (textureSet.isValid()) {
                textureSets.put(surfaceType, textureSet);
                Gdx.app.log("MaterialManager", "Loaded " + surfaceType.name() + " textures");
            } else {
                Gdx.app.error("MaterialManager", "Failed to load " + surfaceType.name() + " textures");
            }
        }
    }

    /**
     * Creates a realistic wall material with proper PBR-like properties.
     * Uses full texture set including diffuse, normal, AO, roughness, and displacement.
     *
     * @return Wall material
     */
    public Material createWallMaterial() {
        return createMaterial(SurfaceType.WALL);
    }

    /**
     * Creates a realistic floor material with proper PBR-like properties.
     * Uses full texture set including diffuse, normal, AO, roughness, and displacement.
     *
     * @return Floor material
     */
    public Material createFloorMaterial() {
        return createMaterial(SurfaceType.FLOOR);
    }

    /**
     * Creates a realistic roof material with proper PBR-like properties.
     * Uses full texture set including diffuse, normal, AO, roughness, and displacement.
     *
     * @return Roof material
     */
    public Material createRoofMaterial() {
        return createMaterial(SurfaceType.ROOF);
    }

    /**
     * Creates a material for a specific surface type using its full texture set.
     *
     * @param surfaceType The surface type to create material for
     * @return Material with all available texture maps applied
     */
    private Material createMaterial(final SurfaceType surfaceType) {
        final Material material = new Material();
        final TextureSet textureSet = textureSets.get(surfaceType);

        if (textureSet == null || !textureSet.isValid()) {
            Gdx.app.error("MaterialManager", "Invalid texture set for " + surfaceType.name());
            return material;
        }

        // Apply diffuse texture and color tint
        if (textureSet.getDiffuseMap() != null) {
            material.set(TextureAttribute.createDiffuse(textureSet.getDiffuseMap()));
            material.set(ColorAttribute.createDiffuse(surfaceType.getDiffuseTint()));
        }

        // Apply normal map for surface detail
        if (textureSet.getNormalMap() != null) {
            material.set(TextureAttribute.createNormal(textureSet.getNormalMap()));
        }

        // Apply displacement/height map for parallax occlusion mapping (use Bump attribute)
        if (textureSet.getDisplacementMap() != null) {
            material.set(new TextureAttribute(TextureAttribute.Bump, textureSet.getDisplacementMap()));
        }

        // Apply roughness map for per-pixel specular control (use Reflection as proxy)
        // Note: Reflection attribute repurposed for roughness in custom shader
        if (textureSet.getRoughnessMap() != null && textureSet.getDisplacementMap() == null) {
            // Only apply if displacement is not used (to avoid conflicts)
            material.set(new TextureAttribute(TextureAttribute.Reflection, textureSet.getRoughnessMap()));
        }

        // Apply specular map for per-pixel reflection control (use Emissive as proxy)
        // Note: Emissive attribute repurposed for specular map in custom shader
        if (textureSet.getSpecularMap() != null) {
            material.set(new TextureAttribute(TextureAttribute.Emissive, textureSet.getSpecularMap()));
        }

        // Apply specular properties (used as defaults if maps not available)
        material.set(ColorAttribute.createSpecular(surfaceType.getSpecularColor()));
        material.set(FloatAttribute.createShininess(surfaceType.getShininess()));

        // Apply ambient color (AO map enhances this)
        material.set(ColorAttribute.createAmbient(surfaceType.getAmbientColor()));

        // PBR textures fully integrated:
        // - Diffuse map: Base color
        // - Normal map: Surface detail
        // - Displacement map: Parallax occlusion mapping for depth
        // - Roughness map: Per-pixel specular control
        // - Specular map: Per-pixel reflection intensity

        return material;
    }

    /**
     * Creates a material for the Sahur model with self-illumination.
     *
     * @param sahurTexture The Sahur texture
     * @return Sahur material
     */
    public Material createSahurMaterial(final Texture sahurTexture) {
        final Material material = new Material();

        // Diffuse texture
        material.set(TextureAttribute.createDiffuse(sahurTexture));
        material.set(ColorAttribute.createDiffuse(Color.WHITE));

        // Slight self-illumination for eerie effect
        material.set(ColorAttribute.createEmissive(new Color(0.1f, 0.02f, 0.02f, 1f)));

        // No specular - creature should look organic/matte
        material.set(ColorAttribute.createSpecular(new Color(0.0f, 0.0f, 0.0f, 1f)));
        material.set(FloatAttribute.createShininess(0f));

        return material;
    }

    /**
     * Disposes of all loaded textures.
     */
    public void dispose() {
        for (final TextureSet textureSet : textureSets.values()) {
            textureSet.dispose();
        }
        textureSets.clear();
    }
}

