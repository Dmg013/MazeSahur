package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.mazesahur.model.*;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Loot crate shop screen.
 */
public class LootCratesScreen extends ScalableGameScreen {

    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;

    // Colors matching MenuScreen
    private static final Color BUTTON_COLOR = new Color(0.12f, 0.12f, 0.15f, 0.95f);
    private static final Color BUTTON_HOVER_COLOR = new Color(0.5f, 0.05f, 0.05f, 0.95f);
    private static final Color BUTTON_BORDER_COLOR = new Color(0.7f, 0.1f, 0.1f, 1.0f);
    private static final Color BUTTON_BORDER_HOVER = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color TEXT_DIM = new Color(0.6f, 0.6f, 0.6f, 0.8f);
    private static final Color COIN_COLOR = new Color(1.0f, 0.8f, 0.0f, 1.0f);

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;
    private GlyphLayout layout;

    private Rectangle basicCrateButton;
    private Rectangle premiumCrateButton;
    private Rectangle eliteCrateButton;
    private Rectangle backButton;

    private CurrencyManager currencyManager;
    private UnlockManager unlockManager;
    private CrateOpeningService crateService;

    private String message = "";
    private float messageTimer = 0f;

    public LootCratesScreen() {
        super(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(4.0f);
        titleFont.setColor(TITLE_COLOR);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.0f);
        buttonFont.setColor(TEXT_COLOR);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.3f);
        smallFont.setColor(TEXT_DIM);

        // Initialize managers
        currencyManager = new CurrencyManager();
        unlockManager = new UnlockManager();
        crateService = new CrateOpeningService(unlockManager, currencyManager);

        // Initialize buttons
        int buttonWidth = 300;
        int buttonHeight = 120;
        int spacing = 40;
        int startX = (VIEWPORT_WIDTH - (buttonWidth * 3 + spacing * 2)) / 2;
        int buttonY = VIEWPORT_HEIGHT / 2;

        basicCrateButton = new Rectangle(startX, buttonY, buttonWidth, buttonHeight);
        premiumCrateButton = new Rectangle(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);
        eliteCrateButton = new Rectangle(startX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight);

        backButton = new Rectangle(40, 40, 150, 60);

        System.out.println("[LootCratesScreen] Initialized");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // Update message timer
        if (messageTimer > 0) {
            messageTimer -= delta;
        }

        // Handle ESC key
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            GameApp.switchScreen("Menu");
            return;
        }

        // Cheat: Press C to add 1000 coins (for debugging)
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            currencyManager.earnCoins(1000, "cheat");
            showMessage("Cheat: +1000 coins!");
        }

        // Clear screen
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int mouseX = Gdx.input.getX();
        int mouseY = VIEWPORT_HEIGHT - Gdx.input.getY();

        boolean basicHovered = basicCrateButton.contains(mouseX, mouseY);
        boolean premiumHovered = premiumCrateButton.contains(mouseX, mouseY);
        boolean eliteHovered = eliteCrateButton.contains(mouseX, mouseY);
        boolean backHovered = backButton.contains(mouseX, mouseY);

        // Handle clicks
        if (Gdx.input.justTouched()) {
            if (basicHovered) {
                openCrate(CrateType.BASIC);
            } else if (premiumHovered) {
                openCrate(CrateType.PREMIUM);
            } else if (eliteHovered) {
                openCrate(CrateType.ELITE);
            } else if (backHovered) {
                GameApp.switchScreen("Menu");
                return;
            }
        }

        // Enable blending
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw buttons
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        drawCrateButton(basicCrateButton, basicHovered, CrateType.BASIC);
        drawCrateButton(premiumCrateButton, premiumHovered, CrateType.PREMIUM);
        drawCrateButton(eliteCrateButton, eliteHovered, CrateType.ELITE);

        shapeRenderer.setColor(backHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);

        shapeRenderer.end();

        // Draw borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);

        drawCrateBorder(basicCrateButton, basicHovered);
        drawCrateBorder(premiumCrateButton, premiumHovered);
        drawCrateBorder(eliteCrateButton, eliteHovered);

        shapeRenderer.setColor(backHovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER_COLOR);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);

        shapeRenderer.end();

        // Draw text
        batch.begin();

        // Title
        titleFont.setColor(TITLE_COLOR);
        layout.setText(titleFont, "LOOT CRATES");
        titleFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2, VIEWPORT_HEIGHT - 60);

        // Coin balance (large and centered)
        buttonFont.setColor(COIN_COLOR);
        String coinText = "COINS: " + currencyManager.getBalance();
        layout.setText(buttonFont, coinText);
        buttonFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2, VIEWPORT_HEIGHT - 140);

        // Crate labels and costs
        drawCrateText(basicCrateButton, "BASIC CRATE", CrateType.BASIC.getCost());
        drawCrateText(premiumCrateButton, "PREMIUM CRATE", CrateType.PREMIUM.getCost());
        drawCrateText(eliteCrateButton, "ELITE CRATE", CrateType.ELITE.getCost());

        // Back button
        buttonFont.setColor(Color.WHITE);
        layout.setText(buttonFont, "BACK");
        buttonFont.draw(batch, layout,
            backButton.x + (backButton.width - layout.width) / 2,
            backButton.y + (backButton.height + layout.height) / 2);

        // Collection status
        smallFont.setColor(TEXT_DIM);
        String collectionText = "Collection: " + unlockManager.getUnlockedCount() + "/" + unlockManager.getTotalCharacterCount() + " Characters";
        layout.setText(smallFont, collectionText);
        smallFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2, 140);

        // Cheat hint (bottom right)
        smallFont.getData().setScale(0.8f);
        smallFont.setColor(new Color(0.3f, 0.3f, 0.3f, 0.5f));
        String cheatHint = "Press C for +1000 coins (debug)";
        layout.setText(smallFont, cheatHint);
        smallFont.draw(batch, layout, VIEWPORT_WIDTH - layout.width - 20, 30);
        smallFont.getData().setScale(1.3f);

        // Message (if any)
        if (messageTimer > 0) {
            float alpha = Math.min(1.0f, messageTimer);
            buttonFont.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha);
            layout.setText(buttonFont, message);
            buttonFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2, VIEWPORT_HEIGHT / 2 - 150);
        }

        batch.end();
    }

    private void drawCrateButton(Rectangle button, boolean hovered, CrateType type) {
        boolean canAfford = currencyManager.getBalance() >= type.getCost();
        Color bgColor;

        if (!canAfford) {
            bgColor = new Color(0.08f, 0.08f, 0.1f, 0.95f); // Darker if can't afford
        } else if (hovered) {
            bgColor = BUTTON_HOVER_COLOR;
        } else {
            bgColor = BUTTON_COLOR;
        }

        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
    }

    private void drawCrateBorder(Rectangle button, boolean hovered) {
        shapeRenderer.setColor(hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER_COLOR);
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
    }

    private void drawCrateText(Rectangle button, String name, int cost) {
        buttonFont.setColor(TEXT_COLOR);
        layout.setText(buttonFont, name);
        buttonFont.draw(batch, layout,
            button.x + (button.width - layout.width) / 2,
            button.y + button.height - 30);

        boolean canAfford = currencyManager.getBalance() >= cost;
        smallFont.setColor(canAfford ? COIN_COLOR : new Color(0.6f, 0.1f, 0.1f, 0.8f));
        String costText = cost + " COINS";
        layout.setText(smallFont, costText);
        smallFont.draw(batch, layout,
            button.x + (button.width - layout.width) / 2,
            button.y + 40);
    }

    private void openCrate(CrateType type) {
        if (!currencyManager.spendCoins(type.getCost())) {
            showMessage("Not enough coins!");
            return;
        }

        LootReward reward = crateService.openCrate(type);

        // Show animated opening screen
        CrateOpeningScreen openingScreen = new CrateOpeningScreen(type, reward, currencyManager, crateService);
        GameApp.addScreen("CrateOpening", openingScreen);
        GameApp.switchScreen("CrateOpening");
    }

    private void showMessage(String msg) {
        message = msg;
        messageTimer = 3.0f;
    }

    @Override
    public void hide() {
        // Called when screen is no longer active
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
