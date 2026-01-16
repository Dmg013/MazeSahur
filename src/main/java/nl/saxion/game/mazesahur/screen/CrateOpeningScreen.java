package nl.saxion.game.mazesahur.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import nl.saxion.game.mazesahur.model.*;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated crate opening screen with slot machine-style rolling.
 */
public class CrateOpeningScreen extends ScalableGameScreen {

    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;
    private final Matrix4 uiProjection = new Matrix4();
    private final Vector2 mouseBuffer = new Vector2();
    private float viewportScale = 1f;
    private float viewportWidth = VIEWPORT_WIDTH;
    private float viewportHeight = VIEWPORT_HEIGHT;
    private float viewportX = 0f;
    private float viewportY = 0f;

    // Animation phases
    private enum Phase {
        ANTICIPATION,  // Crate shaking (1s)
        ROLLING,       // Items scrolling (3s)
        SLOWDOWN,      // Dramatic slowdown (2s)
        REVEAL,        // Show result (2s)
        DONE           // Wait for user input
    }

    private Phase currentPhase = Phase.ANTICIPATION;
    private float phaseTimer = 0f;

    // Scrolling items
    private List<CharacterType> scrollItems;
    private int actualItemIndex = 15; // Position of actual reward in scroll
    private float scrollOffset = 0f;
    private float scrollSpeed = 0f;
    private static final float ITEM_WIDTH = 200f;
    private static final float ITEM_HEIGHT = 250f;
    private float slowdownStartOffset = 0f;
    private float slowdownTargetOffset = 0f;

    // Colors
    private static final Color TITLE_COLOR = new Color(0.9f, 0.15f, 0.15f, 1.0f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color PANEL_BG = new Color(0.08f, 0.08f, 0.1f, 0.92f);

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;
    private GlyphLayout layout;

    private final CrateType crateType;
    private final LootReward reward;
    private final CurrencyManager currencyManager;
    private final CrateOpeningService crateService;

    private Rectangle rollAgainButton;

    private float shakeIntensity = 0f;
    private Random random = new Random();

    public CrateOpeningScreen(CrateType crateType, LootReward reward,
                              CurrencyManager currencyManager, CrateOpeningService crateService) {
        super(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        this.crateType = crateType;
        this.reward = reward;
        this.currencyManager = currencyManager;
        this.crateService = crateService;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(4.0f);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.5f);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.5f);

        rollAgainButton = new Rectangle(VIEWPORT_WIDTH / 2f - 180, 60, 360, 80);

        // Generate scroll items - heavily weighted to show good items (creates desire)
        generateScrollItems();

        updateViewportTransform();
        System.out.println("[CrateOpeningScreen] Opening " + crateType.getDisplayName());
    }

    private void generateScrollItems() {
        scrollItems = new ArrayList<>();
        CharacterType[] allChars = CharacterType.values();

        // Create 20 items total
        for (int i = 0; i < 20; i++) {
            if (i == actualItemIndex) {
                // This is the actual reward
                scrollItems.add(reward.getCharacter());
            } else {
                // Fill with random items, heavily biased toward rare items to create desire
                // Show lots of legendary/epic items that they "almost" got
                double roll = random.nextDouble() * 100;

                if (roll < 30) {
                    // 30% legendary in scroll (way higher than actual drop rate!)
                    scrollItems.add(getRandomCharacterOfRarity(Rarity.LEGENDARY));
                } else if (roll < 55) {
                    // 25% epic
                    scrollItems.add(getRandomCharacterOfRarity(Rarity.EPIC));
                } else if (roll < 80) {
                    // 25% rare
                    scrollItems.add(getRandomCharacterOfRarity(Rarity.RARE));
                } else {
                    // 20% common
                    scrollItems.add(getRandomCharacterOfRarity(Rarity.COMMON));
                }
            }
        }
    }

    private CharacterType getRandomCharacterOfRarity(Rarity rarity) {
        List<CharacterType> candidates = new ArrayList<>();
        for (CharacterType type : CharacterType.values()) {
            if (type.getRarity() == rarity) {
                candidates.add(type);
            }
        }
        if (candidates.isEmpty()) {
            return CharacterType.DEFAULT;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        updateViewportTransform();
        phaseTimer += delta;

        updatePhase(delta);

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);
        batch.setProjectionMatrix(uiProjection);
        shapeRenderer.setProjectionMatrix(uiProjection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (currentPhase == Phase.ANTICIPATION) {
            renderAnticipation();
        } else if (currentPhase == Phase.ROLLING || currentPhase == Phase.SLOWDOWN) {
            renderRolling(delta);
        } else if (currentPhase == Phase.REVEAL || currentPhase == Phase.DONE) {
            renderReveal();
        }
    }

    private void updatePhase(float delta) {
        switch (currentPhase) {
            case ANTICIPATION:
                shakeIntensity = phaseTimer * 0.5f;
                if (phaseTimer >= 1.0f) {
                    currentPhase = Phase.ROLLING;
                    phaseTimer = 0f;
                    scrollSpeed = 3000f; // Start very fast
                    System.out.println("[CrateOpening] Entering ROLLING phase");
                }
                break;

            case ROLLING:
                scrollOffset += scrollSpeed * delta;
                if (phaseTimer >= 2.5f) {
                    currentPhase = Phase.SLOWDOWN;
                    phaseTimer = 0f;
                    slowdownStartOffset = scrollOffset;
                    slowdownTargetOffset = calculateSlowdownTargetOffset(scrollOffset);
                    System.out.println("[CrateOpening] Entering SLOWDOWN phase. ScrollOffset: " + scrollOffset);
                }
                break;

            case SLOWDOWN:
                // Exponential slowdown
                float progress = Math.min(1f, phaseTimer / 2.5f);
                scrollOffset = Interpolation.exp10Out.apply(slowdownStartOffset, slowdownTargetOffset, progress);

                // Smoothly align to target
                if (progress >= 1f) {
                    scrollOffset = slowdownTargetOffset;
                    currentPhase = Phase.REVEAL;
                    phaseTimer = 0f;
                    System.out.println("[CrateOpening] Entering REVEAL phase. Final offset: " + scrollOffset);
                }
                break;

            case REVEAL:
                if (phaseTimer >= 2.5f) {
                    currentPhase = Phase.DONE;
                    phaseTimer = 0f;
                    System.out.println("[CrateOpening] Entering DONE phase");
                }
                break;

            case DONE:
                if (Gdx.input.justTouched()) {
                    final Vector2 mousePos = getMouseInViewport();
                    final int mouseX = (int) mousePos.x;
                    final int mouseY = (int) mousePos.y;
                    boolean canAfford = currencyManager.getBalance() >= crateType.getCost();
                    if (rollAgainButton.contains(mouseX, mouseY) && canAfford) {
                        if (currencyManager.spendCoins(crateType.getCost())) {
                            LootReward nextReward = crateService.openCrate(crateType);
                            CrateOpeningScreen openingScreen = new CrateOpeningScreen(crateType, nextReward, currencyManager, crateService);
                            GameApp.addScreen("CrateOpening", openingScreen);
                            GameApp.switchScreen("CrateOpening");
                        }
                    } else {
                        GameApp.switchScreen("LootCrates");
                    }
                } else if (phaseTimer >= 5.0f) {
                    GameApp.switchScreen("LootCrates");
                }
                break;
        }
    }

    private void renderAnticipation() {
        // Shaking crate
        float shakeX = (random.nextFloat() - 0.5f) * shakeIntensity * 20f;
        float shakeY = (random.nextFloat() - 0.5f) * shakeIntensity * 20f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PANEL_BG);
        float crateSize = 300f;
        shapeRenderer.rect(
            VIEWPORT_WIDTH / 2f - crateSize / 2f + shakeX,
            VIEWPORT_HEIGHT / 2f - crateSize / 2f + shakeY,
            crateSize, crateSize
        );
        shapeRenderer.end();

        batch.begin();
        titleFont.setColor(TITLE_COLOR);
        layout.setText(titleFont, "Opening " + crateType.getDisplayName() + "...");
        titleFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2, VIEWPORT_HEIGHT / 2 + 200);
        batch.end();
    }

    private float calculateSlowdownTargetOffset(float currentOffset) {
        int size = scrollItems.size();
        float centerOffset = VIEWPORT_WIDTH / 2f - ITEM_WIDTH / 2f;
        float currentIndex = (currentOffset + centerOffset) / ITEM_WIDTH;
        int currentFloor = (int) Math.floor(currentIndex);
        int offsetToDesired = Math.floorMod(actualItemIndex - currentFloor, size);
        if (offsetToDesired == 0) {
            offsetToDesired = size; // Ensure forward motion in slowdown.
        }
        int targetIndex = currentFloor + offsetToDesired;
        return targetIndex * ITEM_WIDTH - centerOffset;
    }

    private void renderRolling(float delta) {
        // Draw scrolling items
        float centerX = VIEWPORT_WIDTH / 2f;
        float centerY = VIEWPORT_HEIGHT / 2f;

        // Dark background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1.0f);
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        shapeRenderer.end();

        // Draw items (infinite scrolling with looping)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Calculate visible range
        int visibleStart = (int)(scrollOffset / ITEM_WIDTH) - 2;
        int visibleEnd = (int)((scrollOffset + VIEWPORT_WIDTH) / ITEM_WIDTH) + 2;

        for (int i = visibleStart; i <= visibleEnd; i++) {
            // Wrap index around the scrollItems list for infinite scrolling
            int wrappedIndex = ((i % scrollItems.size()) + scrollItems.size()) % scrollItems.size();
            CharacterType character = scrollItems.get(wrappedIndex);
            Rarity rarity = character.getRarity();

            float itemX = i * ITEM_WIDTH - scrollOffset;

            // Distance from center (for scaling effect)
            float distFromCenter = Math.abs(itemX + ITEM_WIDTH / 2f - centerX);
            float scale = 1.0f - (distFromCenter / VIEWPORT_WIDTH) * 0.3f;
            scale = Math.max(0.7f, scale);

            float scaledWidth = ITEM_WIDTH * scale;
            float scaledHeight = ITEM_HEIGHT * scale;
            float scaledX = itemX + (ITEM_WIDTH - scaledWidth) / 2f;
            float scaledY = centerY - scaledHeight / 2f;

            // Draw item background with rarity color
            Color rarityColor = rarity.getColor();
            shapeRenderer.setColor(rarityColor.r * 0.3f, rarityColor.g * 0.3f, rarityColor.b * 0.3f, 0.9f);
            shapeRenderer.rect(scaledX, scaledY, scaledWidth, scaledHeight);

            // Draw border
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(3f);
            shapeRenderer.setColor(rarityColor);
            shapeRenderer.rect(scaledX, scaledY, scaledWidth, scaledHeight);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        }

        shapeRenderer.end();

        // Draw text on items (infinite scrolling with looping)
        batch.begin();
        for (int i = visibleStart; i <= visibleEnd; i++) {
            // Wrap index around the scrollItems list for infinite scrolling
            int wrappedIndex = ((i % scrollItems.size()) + scrollItems.size()) % scrollItems.size();
            CharacterType character = scrollItems.get(wrappedIndex);
            Rarity rarity = character.getRarity();

            float itemX = i * ITEM_WIDTH - scrollOffset;

            float distFromCenter = Math.abs(itemX + ITEM_WIDTH / 2f - centerX);
            float scale = 1.0f - (distFromCenter / VIEWPORT_WIDTH) * 0.3f;
            scale = Math.max(0.7f, scale);

            float scaledX = itemX + (ITEM_WIDTH - ITEM_WIDTH * scale) / 2f;
            float scaledY = centerY;

            smallFont.getData().setScale(1.2f * scale);
            smallFont.setColor(TEXT_COLOR);
            layout.setText(smallFont, character.getDisplayName());
            smallFont.draw(batch, layout,
                scaledX + (ITEM_WIDTH * scale - layout.width) / 2f,
                scaledY + 20);

            smallFont.getData().setScale(0.9f * scale);
            smallFont.setColor(rarity.getColor());
            layout.setText(smallFont, rarity.getDisplayName());
            smallFont.draw(batch, layout,
                scaledX + (ITEM_WIDTH * scale - layout.width) / 2f,
                scaledY - 20);

            smallFont.getData().setScale(1.5f); // Reset
        }

        // Title at top
        titleFont.setColor(TITLE_COLOR);
        layout.setText(titleFont, "Opening " + crateType.getDisplayName() + "...");
        titleFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, VIEWPORT_HEIGHT - 60);

        // Debug info
        smallFont.getData().setScale(1.0f);
        smallFont.setColor(new Color(0.5f, 0.5f, 0.5f, 0.8f));
        String debugText = "Phase: " + currentPhase + " | Items: " + scrollItems.size() + " | Offset: " + (int)scrollOffset;
        layout.setText(smallFont, debugText);
        smallFont.draw(batch, layout, 20, VIEWPORT_HEIGHT - 20);
        smallFont.getData().setScale(1.5f);

        batch.end();

        // Draw center indicator AFTER batch
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(4f);
        shapeRenderer.setColor(TITLE_COLOR);
        shapeRenderer.line(centerX, centerY - 200, centerX, centerY + 200);
        shapeRenderer.end();
    }

    private void renderReveal() {
        float centerX = VIEWPORT_WIDTH / 2f;
        float centerY = VIEWPORT_HEIGHT / 2f;

        // Pulsing effect
        float pulse = 1.0f + MathUtils.sin(phaseTimer * 8f) * 0.1f;

        Rarity rarity = reward.getRarity();
        Color rarityColor = rarity.getColor();

        // Background glow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float glowSize = 400f * pulse;
        shapeRenderer.setColor(rarityColor.r * 0.2f, rarityColor.g * 0.2f, rarityColor.b * 0.2f, 0.6f);
        shapeRenderer.circle(centerX, centerY, glowSize);
        shapeRenderer.end();

        // Item card
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float cardWidth = ITEM_WIDTH * 1.5f * pulse;
        float cardHeight = ITEM_HEIGHT * 1.5f * pulse;
        shapeRenderer.setColor(rarityColor.r * 0.4f, rarityColor.g * 0.4f, rarityColor.b * 0.4f, 0.95f);
        shapeRenderer.rect(centerX - cardWidth / 2f, centerY - cardHeight / 2f, cardWidth, cardHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(5f * pulse);
        shapeRenderer.setColor(rarityColor);
        shapeRenderer.rect(centerX - cardWidth / 2f, centerY - cardHeight / 2f, cardWidth, cardHeight);
        shapeRenderer.end();

        // Text
        batch.begin();

        // Rarity name at top
        titleFont.setColor(rarityColor);
        titleFont.getData().setScale(5.0f * pulse);
        layout.setText(titleFont, rarity.getDisplayName().toUpperCase());
        titleFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, VIEWPORT_HEIGHT - 100);
        titleFont.getData().setScale(4.0f);

        // Character name
        buttonFont.setColor(TEXT_COLOR);
        buttonFont.getData().setScale(2.8f);
        layout.setText(buttonFont, reward.getCharacter().getDisplayName());
        buttonFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, centerY + 50);

        // New or duplicate
        if (reward.isNew()) {
            smallFont.setColor(new Color(0.3f, 1.0f, 0.3f, 1.0f));
            layout.setText(smallFont, "NEW CHARACTER!");
        } else {
            smallFont.setColor(new Color(1.0f, 0.8f, 0.0f, 1.0f));
            layout.setText(smallFont, "DUPLICATE - +" + reward.getCoinValue() + " COINS");
        }
        smallFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, centerY - 50);

        boolean showRollButton = currentPhase == Phase.DONE;
        boolean canAfford = false;
        if (showRollButton) {
            float alpha = MathUtils.sin(phaseTimer * 3f) * 0.5f + 0.5f;
            smallFont.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
            layout.setText(smallFont, "Click anywhere to continue");
            smallFont.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, 100);
            canAfford = currencyManager.getBalance() >= crateType.getCost();
        }

        buttonFont.getData().setScale(2.5f);
        batch.end();

        if (showRollButton) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (canAfford) {
                shapeRenderer.setColor(0.12f, 0.12f, 0.15f, 0.95f);
            } else {
                shapeRenderer.setColor(0.08f, 0.08f, 0.1f, 0.95f);
            }
            shapeRenderer.rect(rollAgainButton.x, rollAgainButton.y, rollAgainButton.width, rollAgainButton.height);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(canAfford ? new Color(0.9f, 0.15f, 0.15f, 1.0f) : new Color(0.4f, 0.1f, 0.1f, 0.6f));
            shapeRenderer.rect(rollAgainButton.x, rollAgainButton.y, rollAgainButton.width, rollAgainButton.height);
            shapeRenderer.end();

            batch.begin();
            buttonFont.setColor(canAfford ? TEXT_COLOR : new Color(0.6f, 0.6f, 0.6f, 0.9f));
            layout.setText(buttonFont, "ROLL AGAIN (" + crateType.getCost() + ")");
            buttonFont.draw(batch, layout,
                rollAgainButton.x + (rollAgainButton.width - layout.width) / 2f,
                rollAgainButton.y + (rollAgainButton.height + layout.height) / 2f);
            batch.end();
        }
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
