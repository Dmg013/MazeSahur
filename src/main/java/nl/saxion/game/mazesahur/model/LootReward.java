package nl.saxion.game.mazesahur.model;

/**
 * Represents a loot drop result from a crate.
 */
public class LootReward {

    private final CharacterType character;
    private final Rarity rarity;
    private final boolean isNew;
    private final int coinValue;

    public LootReward(final CharacterType character, final Rarity rarity,
                      final boolean isNew, final int coinValue) {
        this.character = character;
        this.rarity = rarity;
        this.isNew = isNew;
        this.coinValue = coinValue;
    }

    public CharacterType getCharacter() {
        return character;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public boolean isNew() {
        return isNew;
    }

    public int getCoinValue() {
        return coinValue;
    }

    public boolean isDuplicate() {
        return !isNew;
    }
}
