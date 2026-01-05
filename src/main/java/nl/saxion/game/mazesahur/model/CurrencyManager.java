package nl.saxion.game.mazesahur.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Manages player's coin balance and earning/spending mechanics.
 */
public class CurrencyManager {

    private static final String PREFS_NAME = "MazeSahur";
    private static final int DEBUG_STARTING_COINS = 1000; // For testing (10 basic crates)

    private int coins;

    public CurrencyManager() {
        load();
    }

    public void earnCoins(final int amount, final String reason) {
        if (amount <= 0) {
            return;
        }
        coins += amount;
        save();
        System.out.println("[CurrencyManager] Earned " + amount + " coins (" + reason + "). Total: " + coins);
    }

    public boolean spendCoins(final int amount) {
        if (amount <= 0 || coins < amount) {
            return false;
        }
        coins -= amount;
        save();
        System.out.println("[CurrencyManager] Spent " + amount + " coins. Remaining: " + coins);
        return true;
    }

    public int getBalance() {
        return coins;
    }

    private void save() {
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putInteger("coins", coins);
        prefs.flush();
    }

    private void load() {
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Check if this is first time (no coins saved)
        if (!prefs.contains("coins")) {
            // First time - give debug starting coins
            coins = DEBUG_STARTING_COINS;
            System.out.println("[CurrencyManager] First time - awarded " + DEBUG_STARTING_COINS + " debug coins");
            save();
        } else {
            coins = prefs.getInteger("coins", 0);
            System.out.println("[CurrencyManager] Loaded " + coins + " coins");
        }
    }
}
