package nl.saxion.game;

import nl.saxion.game.mazesahur.FirstPersonMazeScreen;
import nl.saxion.gameapp.GameApp;

/**
 * Main entry point for the MazeSahur game.
 * A first-person 3D maze game with procedural generation.
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
        GameApp.addScreen("MazeGame", new FirstPersonMazeScreen());

        // Start game loop
        GameApp.start("MazeSahur - First Person Maze", 1280, 720, 60, false, "MazeGame");
    }
}
