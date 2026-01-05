package nl.saxion.game.mazesahur.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Drop probability table for loot crates.
 */
public class DropTable {

    private final double commonChance;
    private final double rareChance;
    private final double epicChance;
    private final double legendaryChance;

    public DropTable(final double commonChance, final double rareChance,
                     final double epicChance, final double legendaryChance) {
        this.commonChance = commonChance;
        this.rareChance = rareChance;
        this.epicChance = epicChance;
        this.legendaryChance = legendaryChance;
    }

    public Rarity rollRarity(final Random random) {
        final double roll = random.nextDouble() * 100.0;
        double cumulative = 0.0;

        cumulative += legendaryChance;
        if (roll < cumulative) {
            System.out.println("[DropTable] Rolled LEGENDARY!");
            return Rarity.LEGENDARY;
        }

        cumulative += epicChance;
        if (roll < cumulative) {
            System.out.println("[DropTable] Rolled Epic");
            return Rarity.EPIC;
        }

        cumulative += rareChance;
        if (roll < cumulative) {
            System.out.println("[DropTable] Rolled Rare");
            return Rarity.RARE;
        }

        System.out.println("[DropTable] Rolled Common");
        return Rarity.COMMON;
    }

    public CharacterType rollCharacter(final Rarity rarity, final Random random) {
        final List<CharacterType> candidates = new ArrayList<>();
        for (CharacterType type : CharacterType.values()) {
            if (type.getRarity() == rarity) {
                candidates.add(type);
            }
        }

        if (candidates.isEmpty()) {
            return CharacterType.DEFAULT;
        }

        final CharacterType result = candidates.get(random.nextInt(candidates.size()));
        System.out.println("[DropTable] Rolled: " + result.getDisplayName());
        return result;
    }
}
