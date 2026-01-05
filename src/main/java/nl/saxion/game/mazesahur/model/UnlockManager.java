package nl.saxion.game.mazesahur.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages which characters are unlocked.
 */
public class UnlockManager {

    private static final String PREFS_NAME = "MazeSahur";
    private Set<CharacterType> unlockedCharacters;

    public UnlockManager() {
        unlockedCharacters = new HashSet<>();
        load();
    }

    public boolean isUnlocked(final CharacterType character) {
        return unlockedCharacters.contains(character);
    }

    public void unlock(final CharacterType character) {
        if (unlockedCharacters.add(character)) {
            System.out.println("[UnlockManager] Unlocked: " + character.getDisplayName());
            save();
        }
    }

    public Set<CharacterType> getUnlockedCharacters() {
        return new HashSet<>(unlockedCharacters);
    }

    public int getUnlockedCount() {
        return unlockedCharacters.size();
    }

    public int getTotalCharacterCount() {
        return CharacterType.values().length;
    }

    private void save() {
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        for (CharacterType type : CharacterType.values()) {
            prefs.putBoolean("unlocked_" + type.name(), unlockedCharacters.contains(type));
        }
        prefs.flush();
    }

    private void load() {
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        unlockedCharacters.clear();

        // DEFAULT is always unlocked
        unlockedCharacters.add(CharacterType.DEFAULT);

        // Load other characters
        for (CharacterType type : CharacterType.values()) {
            if (type == CharacterType.DEFAULT) continue;
            if (prefs.getBoolean("unlocked_" + type.name(), false)) {
                unlockedCharacters.add(type);
            }
        }

        System.out.println("[UnlockManager] Loaded " + unlockedCharacters.size() + "/" + CharacterType.values().length + " characters");
    }
}
