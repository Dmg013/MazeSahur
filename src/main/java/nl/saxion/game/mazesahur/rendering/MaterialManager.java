package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

/**
 * Manages material creation and texture loading for realistic PBR-like rendering.
 * Handles diffuse, normal, and specular properties for proper light interaction.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class MaterialManager {

    // Texture references
    private Texture wallDiffuse;
    private Texture wallNormal;
    private Texture floorDiffuse;
    private Texture floorNormal;

    /**
     * Loads all textures with proper filtering and wrapping.
     */
    public void loadTextures() {
        // Load wall textures
        wallDiffuse = loadTexture("textures/wall/mossy_brick_diff_4k.jpg");
        wallNormal = loadTexture("textures/wall/mossy_brick_nor_gl_4k.jpg");

        // Load floor textures
        floorDiffuse = loadTexture("textures/floor/concrete_floor_damaged_01_diff_4k.jpg");
        floorNormal = loadTexture("textures/floor/concrete_floor_damaged_01_nor_gl_4k.jpg");
    }

    /**
     * Loads a texture with mipmaps and proper filtering.
     *
     * @param path Internal file path
     * @return Loaded texture
     */
    private Texture loadTexture(final String path) {
        final Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return texture;
    }

    /**
     * Creates a realistic wall material with proper PBR-like properties.
     * Rough stone with very minimal reflectivity.
     *
     * @return Wall material
     */
    public Material createWallMaterial() {
        final Material material = new Material();

        // Diffuse texture and color (lighter to reflect flashlight better)
        material.set(TextureAttribute.createDiffuse(wallDiffuse));
        material.set(ColorAttribute.createDiffuse(new Color(0.75f, 0.75f, 0.75f, 1f)));

        // Normal map for surface detail
        material.set(TextureAttribute.createNormal(wallNormal));

        // Low specular - rough stone barely reflects
        material.set(ColorAttribute.createSpecular(new Color(0.05f, 0.05f, 0.05f, 1f)));
        material.set(FloatAttribute.createShininess(2.0f));

        // Moderate ambient to catch light
        material.set(ColorAttribute.createAmbient(new Color(0.5f, 0.5f, 0.5f, 1f)));

        return material;
    }

    /**
     * Creates a realistic floor material with proper PBR-like properties.
     * Rough concrete with minimal reflectivity and slight dampness.
     *
     * @return Floor material
     */
    public Material createFloorMaterial() {
        final Material material = new Material();

        // Diffuse texture and color (lighter to reflect flashlight better)
        material.set(TextureAttribute.createDiffuse(floorDiffuse));
        material.set(ColorAttribute.createDiffuse(new Color(0.85f, 0.85f, 0.88f, 1f)));

        // Normal map for surface detail
        material.set(TextureAttribute.createNormal(floorNormal));

        // Low specular - damp concrete has slight sheen
        material.set(ColorAttribute.createSpecular(new Color(0.08f, 0.08f, 0.08f, 1f)));
        material.set(FloatAttribute.createShininess(2.5f));

        // Higher ambient to catch more light
        material.set(ColorAttribute.createAmbient(new Color(0.6f, 0.6f, 0.65f, 1f)));

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
        if (wallDiffuse != null) {
            wallDiffuse.dispose();
        }
        if (wallNormal != null) {
            wallNormal.dispose();
        }
        if (floorDiffuse != null) {
            floorDiffuse.dispose();
        }
        if (floorNormal != null) {
            floorNormal.dispose();
        }
    }
}

