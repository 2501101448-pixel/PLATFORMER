package levels;

public class EnemySpawn {

    private final EnemyType type;
    private final int column;
    private final int row;

    public EnemySpawn(EnemyType type, int column, int row) {
        if (type == null) {
            throw new IllegalArgumentException("Enemy type is required.");
        }
        this.type = type;
        this.column = column;
        this.row = row;
    }

    public EnemyType getType() {
        return type;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }
}
