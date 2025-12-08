package nl.saxion.game.mazesahur.net;

import nl.saxion.game.mazesahur.model.CharacterType;

/**
 * Immutable config object for joining a multiplayer room.
 */
public class NetworkSessionConfig {
    private final String serverUrl;
    private final String roomId;
    private final String playerName;
    private final CharacterType characterType;

    public NetworkSessionConfig(final String serverUrl,
                                final String roomId,
                                final String playerName,
                                final CharacterType characterType) {
        this.serverUrl = serverUrl;
        this.roomId = roomId;
        this.playerName = playerName;
        this.characterType = characterType != null ? characterType : CharacterType.DEFAULT;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }
}
