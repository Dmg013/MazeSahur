package nl.saxion.game.mazesahur.ai;

import nl.saxion.game.mazesahur.world.Maze;

import java.util.*;

/**
 * A* pathfinding service for navigating the maze.
 * Provides static methods for finding optimal paths and checking line of sight.
 *
 * @author Olivier, Luuk, Russell, Tim
 * @version 1.0
 */
public final class PathfindingService {

    private PathfindingService() {
        throw new AssertionError("Cannot instantiate PathfindingService");
    }

    /**
     * Finds the shortest path from start to end using A* algorithm.
     *
     * @param maze  The maze to navigate
     * @param startX Start grid X coordinate
     * @param startZ Start grid Z coordinate
     * @param endX   End grid X coordinate
     * @param endZ   End grid Z coordinate
     * @return List of grid coordinates representing the path, or empty list if no path exists
     */
    public static List<int[]> findPath(final Maze maze, final int startX, final int startZ,
                                        final int endX, final int endZ) {
        final PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        final Set<String> closedSet = new HashSet<>();
        final Map<String, Node> allNodes = new HashMap<>();

        final Node startNode = new Node(startX, startZ);
        startNode.gScore = 0;
        startNode.fScore = heuristic(startX, startZ, endX, endZ);

        openSet.add(startNode);
        allNodes.put(startNode.key(), startNode);

        while (!openSet.isEmpty()) {
            final Node current = openSet.poll();

            if (current.x == endX && current.z == endZ) {
                return reconstructPath(current);
            }

            closedSet.add(current.key());

            // Check 4 neighbors (N, E, S, W)
            final int[][] neighbors = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (final int[] dir : neighbors) {
                final int nx = current.x + dir[0];
                final int nz = current.z + dir[1];

                if (maze.isWall(nx, nz) || closedSet.contains(Node.makeKey(nx, nz))) {
                    continue;
                }

                final double tentativeGScore = current.gScore + 1;

                final String neighborKey = Node.makeKey(nx, nz);
                Node neighbor = allNodes.get(neighborKey);

                if (neighbor == null) {
                    neighbor = new Node(nx, nz);
                    allNodes.put(neighborKey, neighbor);
                }

                if (tentativeGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = tentativeGScore + heuristic(nx, nz, endX, endZ);

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Checks if there is a clear line of sight between two world positions.
     *
     * @param maze     The maze
     * @param startX   Start world X coordinate
     * @param startZ   Start world Z coordinate
     * @param endX     End world X coordinate
     * @param endZ     End world Z coordinate
     * @param cellSize Size of each grid cell
     * @return True if line of sight is clear
     */
    public static boolean hasLineOfSight(final Maze maze, final float startX, final float startZ,
                                          final float endX, final float endZ, final float cellSize) {
        final float dx = endX - startX;
        final float dz = endZ - startZ;
        final float distance = (float) Math.sqrt(dx * dx + dz * dz);

        final int steps = (int) (distance / (cellSize * 0.5f));

        for (int i = 0; i <= steps; i++) {
            final float t = (float) i / steps;
            final float x = startX + dx * t;
            final float z = startZ + dz * t;

            final int gridX = (int) (x / cellSize);
            final int gridZ = (int) (z / cellSize);

            if (maze.isWall(gridX, gridZ)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Manhattan distance heuristic for A*.
     */
    private static double heuristic(final int x1, final int z1, final int x2, final int z2) {
        return Math.abs(x1 - x2) + Math.abs(z1 - z2);
    }

    /**
     * Reconstructs the path from the end node.
     */
    private static List<int[]> reconstructPath(Node node) {
        final List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(new int[]{node.x, node.z});
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Internal node class for A* pathfinding.
     */
    private static class Node {
        final int x;
        final int z;
        Node parent;
        double gScore = Double.MAX_VALUE;
        double fScore = Double.MAX_VALUE;

        Node(final int x, final int z) {
            this.x = x;
            this.z = z;
        }

        String key() {
            return makeKey(x, z);
        }

        static String makeKey(final int x, final int z) {
            return x + "," + z;
        }
    }
}

