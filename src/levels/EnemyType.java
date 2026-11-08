package levels;

public enum EnemyType {
    BOAR("Boar"),
    SNAIL("Snail"),
    SMALL_BEE("Small Bee");

    private final String displayName;

    EnemyType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
