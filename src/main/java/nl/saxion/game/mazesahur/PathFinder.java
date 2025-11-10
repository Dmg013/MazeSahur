package nl.saxion.game.mazesahur;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* pathfinding algorithm for navigating the maze.
 */
public final class PathFinder {

    /**
     * Private constructor to prevent instantiation.
     */
    private PathFinder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Finds a path from start to goal using A* algorithm.
     *
     * @param maze The maze to navigate
     * @param startX Start X grid position
     * @param startZ Start Z grid position
     * @param goalX Goal X grid position
     * @param goalZ Goal Z grid position
     * @return List of grid positions (x,z pairs) representing the path, or empty if no path
     */
    public static List<int[]> findPath(final MazeGenerator maze, final int startX, final int startZ,
                                       final int goalX, final int goalZ) {
        // Priority queue ordered by f-score (g + h)
        final PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        final Set<String> closedSet = new HashSet<>();
        final Map<String, Node> allNodes = new HashMap<>();

        // Create start node
        final Node startNode = new Node(startX, startZ);
        startNode.gScore = 0;
        startNode.fScore = heuristic(startX, startZ, goalX, goalZ);
        openSet.add(startNode);
        allNodes.put(getKey(startX, startZ), startNode);

        while (!openSet.isEmpty()) {
            final Node current = openSet.poll();
            final String currentKey = getKey(current.x, current.z);

            // Check if we reached the goal
            if (current.x == goalX && current.z == goalZ) {
                return reconstructPath(current);
            }

            closedSet.add(currentKey);

            // Check all 4 neighbors (N, S, E, W)
            final int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

            for (final int[] dir : directions) {
                final int neighborX = current.x + dir[0];
                final int neighborZ = current.z + dir[1];
                final String neighborKey = getKey(neighborX, neighborZ);

                // Skip if wall or out of bounds
                if (maze.isWall(neighborX, neighborZ) || closedSet.contains(neighborKey)) {
                    continue;
                }

                // Calculate tentative g score
                final double tentativeGScore = current.gScore + 1;

                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new Node(neighborX, neighborZ);
                    allNodes.put(neighborKey, neighbor);
                }

                // If this path to neighbor is better than previous one
                if (tentativeGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = neighbor.gScore + heuristic(neighborX, neighborZ, goalX, goalZ);

                    // Add to open set if not already there
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // No path found
        return new ArrayList<>();
    }

    /**
     * Checks if there's a clear line of sight between two points using DDA ray casting.
     *
     * @param maze The maze to check
     * @param x1 Start X world position
     * @param z1 Start Z world position
     * @param x2 End X world position
     * @param z2 End Z world position
     * @param wallSize Size of each wall grid cell
     * @return True if there's clear line of sight, false if blocked by walls
     */
    public static boolean hasLineOfSight(final MazeGenerator maze, final float x1, final float z1,
                                         final float x2, final float z2, final float wallSize) {
        // Calculate direction
        final float dx = x2 - x1;
        final float dz = z2 - z1;
        final float distance = (float) Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.1f) {
            return true; // Same position
        }

        // Normalize direction
        final float dirX = dx / distance;
        final float dirZ = dz / distance;

        // Ray march through the maze
        final int steps = (int) (distance / (wallSize * 0.5f)); // Check every half cell
        for (int i = 0; i <= steps; i++) {
            final float t = (distance / steps) * i;
            final float checkX = x1 + dirX * t;
            final float checkZ = z1 + dirZ * t;

            // Convert to grid coordinates
            final int gridX = (int) (checkX / wallSize);
            final int gridZ = (int) (checkZ / wallSize);

            // Check if this position is a wall
            if (maze.isWall(gridX, gridZ)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates Manhattan distance heuristic.
     *
     * @param x1 Start X
     * @param z1 Start Z
     * @param x2 Goal X
     * @param z2 Goal Z
     * @return Heuristic distance
     */
    private static double heuristic(final int x1, final int z1, final int x2, final int z2) {
        return Math.abs(x1 - x2) + Math.abs(z1 - z2);
    }

    /**
     * Reconstructs the path from goal to start.
     *
     * @param goalNode The goal node
     * @return List of positions forming the path
     */
    private static List<int[]> reconstructPath(final Node goalNode) {
        final List<int[]> path = new ArrayList<>();
        Node current = goalNode;

        while (current != null) {
            path.add(new int[]{current.x, current.z});
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Creates a unique key for a grid position.
     *
     * @param x X position
     * @param z Z position
     * @return Unique key string
     */
    private static String getKey(final int x, final int z) {
        return x + "," + z;
    }

    /**
     * Node class for A* algorithm.
     */
    private static class Node {
        final int x;
        final int z;
        Node parent;
        double gScore = Double.POSITIVE_INFINITY;
        double fScore = Double.POSITIVE_INFINITY;

        Node(final int x, final int z) {
            this.x = x;
            this.z = z;
        }
    }
}
