package nl.saxion.game;

import nl.saxion.game.mazesahur.MainScreen;
import nl.saxion.game.mazesahur.ModelViewerScreen;
import nl.saxion.gameapp.GameApp;

/**
 * Main entry point for the MazeSahur game.
 * Initializes the game screens and starts the game loop.
 */
public final class Main {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Main method that starts the game.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("MainScreen", new MainScreen());
        GameApp.addScreen("ModelViewerScreen", new ModelViewerScreen());

        // Start game loop with 3D model viewer
        GameApp.start("MazeSahur - 3D Model Viewer", 1280, 720, 60, false, "ModelViewerScreen");
    }
}
