package nl.saxion.game.mazesahur;

import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Main game screen for MazeSahur.
 * This is the primary screen where the game logic will be implemented.
 */
public class MainScreen extends ScalableGameScreen {

    /**
     * Constructs a new MainScreen with default resolution.
     * The game world is set to 1280x720 pixels.
     */
    public MainScreen() {
        super(1280, 720);
    }

    /**
     * Called when this screen becomes the active screen.
     * Initialize game resources here.
     */
    @Override
    public void show() {
        // Initialize your game here
    }

    /**
     * Renders the game screen.
     * Called every frame to update and draw the game.
     *
     * @param delta The time in seconds since the last frame
     */
    @Override
    public void render(final float delta) {
        super.render(delta);

        // Clear screen with black
        GameApp.clearScreen("black");
    }

    /**
     * Called when this screen is no longer the active screen.
     * Cleanup resources here.
     */
    @Override
    public void hide() {
        // Cleanup resources here
    }
}