package nl.saxion.game.mazesahur.model;

/**
 * Types of loot crates with different costs and drop rates.
 */
public enum CrateType {
    BASIC("Basic Crate", 100, new DropTable(92.0, 7.0, 0.9, 0.1)),
    PREMIUM("Premium Crate", 250, new DropTable(85.0, 12.0, 2.8, 0.2)),
    ELITE("Elite Crate", 500, new DropTable(75.0, 18.0, 6.0, 1.0));

    private final String displayName;
    private final int cost;
    private final DropTable dropTable;

    CrateType(final String displayName, final int cost, final DropTable dropTable) {
        this.displayName = displayName;
        this.cost = cost;
        this.dropTable = dropTable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public DropTable getDropTable() {
        return dropTable;
    }
}
