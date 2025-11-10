package nl.saxion.game;

import nl.saxion.game.mazesahur.MainScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("MainScreen", new MainScreen());

        // Start game loop
        GameApp.start("MazeSahur", 1280, 720, 60, false, "MainScreen");
    }
}
