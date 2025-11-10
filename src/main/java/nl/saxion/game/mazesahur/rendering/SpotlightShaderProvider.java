package nl.saxion.game.mazesahur.rendering;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

/**
 * Provides the spotlight shader for all renderables.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class SpotlightShaderProvider extends BaseShaderProvider {
    private final SpotlightShader shader;

    public SpotlightShaderProvider(SpotlightShader shader) {
        this.shader = shader;
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        return shader;
    }
}

