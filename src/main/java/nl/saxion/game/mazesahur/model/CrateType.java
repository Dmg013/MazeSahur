package nl.saxion.game.mazesahur.model;

/**
 * Types of loot crates with different costs and drop rates.
 */
public enum CrateType {
    BASIC("Basic Crate", 100, new DropTable(76.0, 20.0, 3.74, 0.26)),
    PREMIUM("Premium Crate", 250, new DropTable(60.0, 30.0, 9.5, 0.5)),
    ELITE("Elite Crate", 500, new DropTable(40.0, 40.0, 18.0, 2.0));

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
