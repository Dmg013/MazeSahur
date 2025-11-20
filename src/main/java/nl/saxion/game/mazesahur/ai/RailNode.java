package nl.saxion.game.mazesahur.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the rail network.
 * Each node can have connections to neighboring nodes in specific directions,
 * creating a rollercoaster-like path system.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class RailNode {
    private final int x;
    private final int z;
    private final Map<RailDirection, RailNode> connections;

    /**
     * Creates a new rail node at the specified grid position.
     *
     * @param x Grid X coordinate
     * @param z Grid Z coordinate
     */
    public RailNode(final int x, final int z) {
        this.x = x;
        this.z = z;
        this.connections = new HashMap<>();
    }

    /**
     * Adds a connection to another rail node in the specified direction.
     *
     * @param direction Direction of the connection
     * @param neighbor The neighboring node
     */
    public void addConnection(final RailDirection direction, final RailNode neighbor) {
        connections.put(direction, neighbor);
    }

    /**
     * Gets the neighboring node in the specified direction.
     *
     * @param direction Direction to check
     * @return The neighboring node, or null if no connection exists
     */
    public RailNode getNeighbor(final RailDirection direction) {
        return connections.get(direction);
    }

    /**
     * Checks if this node has a connection in the specified direction.
     *
     * @param direction Direction to check
     * @return True if connection exists
     */
    public boolean hasConnection(final RailDirection direction) {
        return connections.containsKey(direction);
    }

    /**
     * Gets all available directions from this node.
     *
     * @return Unmodifiable map of directions to neighboring nodes
     */
    public Map<RailDirection, RailNode> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    /**
     * Checks if this is a junction (has more than 2 connections).
     *
     * @return True if this node is a junction
     */
    public boolean isJunction() {
        return connections.size() > 2;
    }

    /**
     * Checks if this is a dead end (has only 1 connection).
     *
     * @return True if this node is a dead end
     */
    public boolean isDeadEnd() {
        return connections.size() == 1;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * Gets a unique key for this node based on its position.
     *
     * @return Unique string key for this node
     */
    public String key() {
        return makeKey(x, z);
    }

    /**
     * Creates a unique key from grid coordinates.
     *
     * @param x Grid X coordinate
     * @param z Grid Z coordinate
     * @return Unique string key for the coordinates
     */
    public static String makeKey(final int x, final int z) {
        return x + "," + z;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RailNode railNode = (RailNode) o;
        return x == railNode.x && z == railNode.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return "RailNode{" + "x=" + x + ", z=" + z + ", connections=" + connections.size() + '}';
    }
}
