package nl.saxion.game;

import nl.saxion.game.mazesahur.screen.GameScreen;
import nl.saxion.gameapp.GameApp;

/**
 * Main entry point for the MazeSahur game.
 * A first-person 3D horror maze game with realistic lighting and AI enemy.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
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
        // Add game screen
        GameApp.addScreen("MazeGame", new GameScreen());

        // Start game loop with configuration
        GameApp.start("MazeSahur - Horror Maze Game", 1280, 720, 60, false, "MazeGame");
    }
}
