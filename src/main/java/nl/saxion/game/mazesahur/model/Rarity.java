package nl.saxion.game.mazesahur.model;

import com.badlogic.gdx.graphics.Color;

/**
 * Enum representing rarity tiers for loot crate drops.
 */
public enum Rarity {
    COMMON("Common", new Color(0.6f, 0.6f, 0.6f, 1f)),
    RARE("Rare", new Color(0.3f, 0.6f, 1.0f, 1f)),
    EPIC("Epic", new Color(0.7f, 0.3f, 1.0f, 1f)),
    LEGENDARY("Legendary", new Color(1.0f, 0.8f, 0.0f, 1f));

    private final String displayName;
    private final Color color;

    Rarity(final String displayName, final Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }
}
