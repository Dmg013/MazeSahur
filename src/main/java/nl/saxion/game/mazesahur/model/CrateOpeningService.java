package nl.saxion.game.mazesahur.model;

import java.util.Random;

/**
 * Service for opening loot crates and generating rewards.
 */
public class CrateOpeningService {

    private final UnlockManager unlockManager;
    private final CurrencyManager currencyManager;
    private final Random random;

    private static final int DUPLICATE_COMMON = 50;
    private static final int DUPLICATE_RARE = 150;
    private static final int DUPLICATE_EPIC = 400;
    private static final int DUPLICATE_LEGENDARY = 1000;

    public CrateOpeningService(final UnlockManager unlockManager, final CurrencyManager currencyManager) {
        this.unlockManager = unlockManager;
        this.currencyManager = currencyManager;
        this.random = new Random();
    }

    public LootReward openCrate(final CrateType crateType) {
        System.out.println("[CrateOpeningService] Opening " + crateType.getDisplayName());

        // Roll rarity
        final Rarity rarity = crateType.getDropTable().rollRarity(random);

        // Roll character
        final CharacterType character = crateType.getDropTable().rollCharacter(rarity, random);

        // Check if new
        final boolean isNew = !unlockManager.isUnlocked(character);

        // Create reward
        final int coinValue = isNew ? 0 : getDuplicateValue(rarity);
        final LootReward reward = new LootReward(character, rarity, isNew, coinValue);

        // Apply reward
        if (isNew) {
            unlockManager.unlock(character);
            System.out.println("[CrateOpeningService] NEW CHARACTER!");
        } else {
            currencyManager.earnCoins(coinValue, "duplicate");
            System.out.println("[CrateOpeningService] Duplicate - awarded " + coinValue + " coins");
        }

        return reward;
    }

    private int getDuplicateValue(final Rarity rarity) {
        switch (rarity) {
            case COMMON: return DUPLICATE_COMMON;
            case RARE: return DUPLICATE_RARE;
            case EPIC: return DUPLICATE_EPIC;
            case LEGENDARY: return DUPLICATE_LEGENDARY;
            default: return 0;
        }
    }
}
