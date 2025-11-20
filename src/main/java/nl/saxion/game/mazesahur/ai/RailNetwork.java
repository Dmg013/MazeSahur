package nl.saxion.game.mazesahur.ai;

import nl.saxion.game.mazesahur.world.Maze;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the rail network for rollercoaster-like pathfinding.
 * Builds a network of connected rail nodes from the maze structure,
 * where movement is constrained to specific directions and rotations.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public class RailNetwork {
    private final Map<String, RailNode> nodes;
    private final Maze maze;

    /**
     * Creates and builds a rail network from the maze.
     *
     * @param maze The maze to build the network from
     */
    public RailNetwork(final Maze maze) {
        this.maze = maze;
        this.nodes = new HashMap<>();
        buildNetwork();
    }

    /**
     * Builds the rail network by creating nodes for all walkable cells
     * and connecting adjacent nodes.
     */
    private void buildNetwork() {
        // Create nodes for all walkable cells
        for (int z = 0; z < maze.getHeight(); z++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (!maze.isWall(x, z)) {
                    final RailNode node = new RailNode(x, z);
                    nodes.put(node.key(), node);
                }
            }
        }

        // Connect adjacent nodes
        for (final RailNode node : nodes.values()) {
            for (final RailDirection direction : RailDirection.values()) {
                final int neighborX = node.getX() + direction.getDx();
                final int neighborZ = node.getZ() + direction.getDz();

                final RailNode neighbor = getNode(neighborX, neighborZ);
                if (neighbor != null) {
                    node.addConnection(direction, neighbor);
                }
            }
        }

        System.out.println("Rail network built: " + nodes.size() + " nodes");
        analyzeNetwork();
    }

    /**
     * Analyzes and prints statistics about the rail network.
     */
    private void analyzeNetwork() {
        int junctions = 0;
        int corridors = 0;
        int deadEnds = 0;

        for (final RailNode node : nodes.values()) {
            final int connections = node.getConnections().size();
            if (connections == 1) {
                deadEnds++;
            } else if (connections == 2) {
                corridors++;
            } else if (connections > 2) {
                junctions++;
            }
        }

        System.out.println("  Junctions: " + junctions);
        System.out.println("  Corridors: " + corridors);
        System.out.println("  Dead ends: " + deadEnds);
    }

    /**
     * Gets the rail node at the specified grid coordinates.
     *
     * @param x Grid X coordinate
     * @param z Grid Z coordinate
     * @return The rail node, or null if none exists
     */
    public RailNode getNode(final int x, final int z) {
        return nodes.get(RailNode.makeKey(x, z));
    }

    /**
     * Gets the closest rail node to the specified world coordinates.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return The closest rail node, or null if network is empty
     */
    public RailNode getClosestNode(final float worldX, final float worldZ) {
        final int[] grid = maze.worldToGrid(worldX, worldZ);
        return getNode(grid[0], grid[1]);
    }

    /**
     * Gets the total number of nodes in the network.
     *
     * @return The number of nodes
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Gets all nodes in the network.
     *
     * @return Unmodifiable map of node keys to rail nodes
     */
    public Map<String, RailNode> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Checks if the network is valid (has at least one node).
     *
     * @return True if the network has at least one node
     */
    public boolean isValid() {
        return !nodes.isEmpty();
    }
}
