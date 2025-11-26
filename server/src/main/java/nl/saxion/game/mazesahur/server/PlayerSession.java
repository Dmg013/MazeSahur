package nl.saxion.game.mazesahur.server;

import io.netty.channel.Channel;

/**
 * Binds a Netty channel to a room and player id.
 */
public class PlayerSession {
    private final Channel channel;
    private final String playerId;
    private final Room room;
    private volatile long lastSeq;

    public PlayerSession(final Channel channel, final String playerId, final Room room) {
        this.channel = channel;
        this.playerId = playerId;
        this.room = room;
        this.lastSeq = 0;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Room getRoom() {
        return room;
    }

    public void setLastSeq(final long seq) {
        this.lastSeq = seq;
    }

    public long getLastSeq() {
        return lastSeq;
    }
}
