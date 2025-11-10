package nl.saxion.game.mazesahur;

import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class MainScreen extends ScalableGameScreen {
    public MainScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        // Initialize your game here
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // Clear screen with black
        GameApp.clearScreen("black");
    }

    @Override
    public void hide() {
        // Cleanup resources here
    }
}