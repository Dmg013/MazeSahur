package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import nl.saxion.game.mazesahur.model.*;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Loot crate shop screen.
 */
public class LootCratesScreen extends ScalableGameScreen {

    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;
    private final Matrix4 uiProjection = new Matrix4();
    private final Vector2 mouseBuffer = new Vector2();
    private float viewportScale = 1f;
    private float viewportWidth = VIEWPORT_WIDTH;
    private float viewportHeight = VIEWPORT_HEIGHT;
    private float viewportX = 0f;
    private float viewportY = 0f;

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

    // Textures
    private Texture backgroundTexture;
    private Texture greenCrateTexture;
    private Texture purpleCrateTexture;
    private Texture goldCrateTexture;
    private Texture titleTexture;
    private Texture coinTexture;

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
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.5f);
        buttonFont.setColor(TEXT_COLOR);
        buttonFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.3f);
        smallFont.setColor(TEXT_DIM);
        smallFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("img/Backgroundl.png"));
        greenCrateTexture = new Texture(Gdx.files.internal("img/groen.png"));
        purpleCrateTexture = new Texture(Gdx.files.internal("img/paars.png"));
        goldCrateTexture = new Texture(Gdx.files.internal("img/Gold.png"));
        titleTexture = new Texture(Gdx.files.internal("img/Lood_crates.png"));
        coinTexture = new Texture(Gdx.files.internal("img/coin.png"));

        // Initialize managers
        currencyManager = new CurrencyManager();
        unlockManager = new UnlockManager();
        crateService = new CrateOpeningService(unlockManager, currencyManager);

        // Initialize buttons (sized to match crate images)
        int buttonWidth = 400;
        int buttonHeight = 400;
        int spacing = 20;
        int startX = (VIEWPORT_WIDTH - (buttonWidth * 3 + spacing * 2)) / 2;
        int buttonY = VIEWPORT_HEIGHT / 2 - 200;

        basicCrateButton = new Rectangle(startX, buttonY, buttonWidth, buttonHeight);
        premiumCrateButton = new Rectangle(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);
        eliteCrateButton = new Rectangle(startX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight);

        backButton = new Rectangle(40, 40, 150, 60);

        updateViewportTransform();
        System.out.println("[LootCratesScreen] Initialized");
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        updateViewportTransform();

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
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);
        batch.setProjectionMatrix(uiProjection);
        shapeRenderer.setProjectionMatrix(uiProjection);

        final Vector2 mousePos = getMouseInViewport();
        final int mouseX = (int) mousePos.x;
        final int mouseY = (int) mousePos.y;

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

        // Draw background
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        batch.end();

        // Draw crate images
        batch.begin();

        // Calculate crate image sizes (larger)
        int crateWidth = 380;
        int crateHeight = 380;

        // Green crate (Basic - 100 coins)
        float greenScale = basicHovered ? 1.08f : 1.0f;
        int greenW = (int)(crateWidth * greenScale);
        int greenH = (int)(crateHeight * greenScale);
        int greenX = (int)(basicCrateButton.x + (basicCrateButton.width - greenW) / 2);
        int greenY = (int)(basicCrateButton.y + (basicCrateButton.height - greenH) / 2);
        batch.draw(greenCrateTexture, greenX, greenY, greenW, greenH);

        // Purple crate (Premium - 250 coins)
        float purpleScale = premiumHovered ? 1.08f : 1.0f;
        int purpleW = (int)(crateWidth * purpleScale);
        int purpleH = (int)(crateHeight * purpleScale);
        int purpleX = (int)(premiumCrateButton.x + (premiumCrateButton.width - purpleW) / 2);
        int purpleY = (int)(premiumCrateButton.y + (premiumCrateButton.height - purpleH) / 2);
        batch.draw(purpleCrateTexture, purpleX, purpleY, purpleW, purpleH);

        // Gold crate (Elite - 500 coins)
        float goldScale = eliteHovered ? 1.08f : 1.0f;
        int goldW = (int)(crateWidth * goldScale);
        int goldH = (int)(crateHeight * goldScale);
        int goldX = (int)(eliteCrateButton.x + (eliteCrateButton.width - goldW) / 2);
        int goldY = (int)(eliteCrateButton.y + (eliteCrateButton.height - goldH) / 2);
        batch.draw(goldCrateTexture, goldX, goldY, goldW, goldH);

        batch.end();

        // Draw back button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);
        shapeRenderer.end();

        // Draw back button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shapeRenderer.setColor(backHovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER_COLOR);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);
        shapeRenderer.end();

        // Draw text
        batch.begin();

        // Title image
        int titleWidth = 650;
        int titleHeight = 320;
        batch.draw(titleTexture, (VIEWPORT_WIDTH - titleWidth) / 2, VIEWPORT_HEIGHT - titleHeight, titleWidth, titleHeight);

        // Coin balance (top left corner) - image + number
        int coinImgWidth = 80;
        int coinImgHeight = 50;
        batch.draw(coinTexture, 25, VIEWPORT_HEIGHT - coinImgHeight - 25, coinImgWidth, coinImgHeight);
        buttonFont.setColor(COIN_COLOR);
        String coinText = "" + currencyManager.getBalance();
        layout.setText(buttonFont, coinText);
        buttonFont.draw(batch, layout, 100, VIEWPORT_HEIGHT - 35);


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
        // Draw crate name at top
        buttonFont.setColor(TEXT_COLOR);
        layout.setText(buttonFont, name);
        buttonFont.draw(batch, layout,
            button.x + (button.width - layout.width) / 2,
            button.y + button.height - 10);

        // Draw cost at bottom
        boolean canAfford = currencyManager.getBalance() >= cost;
        smallFont.setColor(canAfford ? COIN_COLOR : new Color(0.6f, 0.1f, 0.1f, 0.8f));
        String costText = cost + " COINS";
        layout.setText(smallFont, costText);
        smallFont.draw(batch, layout,
            button.x + (button.width - layout.width) / 2,
            button.y + 50);
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

    private void updateViewportTransform() {
        final float windowWidth = Gdx.graphics.getWidth();
        final float windowHeight = Gdx.graphics.getHeight();
        final float scaleX = windowWidth / VIEWPORT_WIDTH;
        final float scaleY = windowHeight / VIEWPORT_HEIGHT;
        viewportScale = Math.min(scaleX, scaleY);
        viewportWidth = VIEWPORT_WIDTH * viewportScale;
        viewportHeight = VIEWPORT_HEIGHT * viewportScale;
        viewportX = (windowWidth - viewportWidth) / 2f;
        viewportY = (windowHeight - viewportHeight) / 2f;

        uiProjection.setToOrtho2D(0f, 0f, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        if (batch != null) {
            batch.setProjectionMatrix(uiProjection);
        }
        if (shapeRenderer != null) {
            shapeRenderer.setProjectionMatrix(uiProjection);
        }
    }

    private Vector2 getMouseInViewport() {
        final float screenX = Gdx.input.getX();
        final float screenY = Gdx.input.getY();
        final float virtualX = (screenX - viewportX) / viewportScale;
        final float virtualY = (Gdx.graphics.getHeight() - screenY - viewportY) / viewportScale;
        return mouseBuffer.set(virtualX, virtualY);
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
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (greenCrateTexture != null) greenCrateTexture.dispose();
        if (purpleCrateTexture != null) purpleCrateTexture.dispose();
        if (goldCrateTexture != null) goldCrateTexture.dispose();
        if (titleTexture != null) titleTexture.dispose();
        if (coinTexture != null) coinTexture.dispose();
    }
}
