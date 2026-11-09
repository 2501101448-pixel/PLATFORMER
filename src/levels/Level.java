package levels;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Level {

    // ==========================================
    // MAP DATA
    // ==========================================

    // Default map dimensions.
    public static final int DEFAULT_COLUMNS = 200;
    public static final int DEFAULT_ROWS = 24;
    public static final int DEFAULT_TILE_SIZE = 32;

    private final int[][] tiles;

    private final int tileSize;

    // Player spawn position, measured in tiles.
    private int spawnColumn = 0;
    private int spawnRow = 0;

    // Background asset name.
    private String backgroundName = "";

    // Enemy spawn information.
    private final List<EnemySpawn> enemySpawns = new ArrayList<>();

    // Tile IDs designated as solid.
    // Only these tile types block player movement.
    private final Set<Integer> solidTileIds = new HashSet<>();


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Level(int columns, int rows, int tileSize) {

        this.tileSize = tileSize;

        tiles = new int[rows][columns];

        // Empty tiles are represented by -1.
        for (int row = 0; row < rows; row++) {

            for (int column = 0; column < columns; column++) {

                tiles[row][column] = -1;
            }
        }
    }


    // ==========================================
    // LOAD MAP
    // ==========================================

    public static Level load(Path file, int tileCount) throws IOException {

        try (Scanner scanner = new Scanner(file)) {

            // First line: columns, rows, tile size.
            int columns = scanner.nextInt();
            int rows = scanner.nextInt();
            int tileSize = scanner.nextInt();

            Level level = new Level(columns, rows, tileSize);

            // Read tile IDs.
            for (int row = 0; row < rows; row++) {

                for (int column = 0; column < columns; column++) {

                    if (scanner.hasNextInt()) {

                        level.tiles[row][column] = scanner.nextInt();

                    } else {

                        throw new IOException(
                                "Map file contains incomplete tile data."
                        );
                    }
                }
            }

            // Read optional map information.
            while (scanner.hasNext()) {

                String section = scanner.next();

                switch (section) {

                    // ----------------------------------
                    // PLAYER SPAWN
                    // ----------------------------------

                    case "SPAWN": {

                        level.spawnColumn = scanner.nextInt();
                        level.spawnRow = scanner.nextInt();

                        break;
                    }

                    // ----------------------------------
                    // BACKGROUND
                    // ----------------------------------

                    case "BACKGROUND": {

                        level.backgroundName = scanner.next();

                        break;
                    }

                    // ----------------------------------
                    // ENEMIES
                    // ----------------------------------

                    case "ENEMIES": {

                        int enemyCount = scanner.nextInt();

                        level.enemySpawns.clear();

                        for (int i = 0; i < enemyCount; i++) {

                            EnemyType type = EnemyType.valueOf(
                                    scanner.next()
                            );

                            int column = scanner.nextInt();
                            int row = scanner.nextInt();

                            level.enemySpawns.add(
                                    new EnemySpawn(type, column, row)
                            );
                        }

                        break;
                    }

                    // ----------------------------------
                    // SOLID TILE TYPES
                    // ----------------------------------

                    case "SOLID_TILES": {

                        int solidCount = scanner.nextInt();

                        level.solidTileIds.clear();

                        for (int i = 0; i < solidCount; i++) {

                            int tileId = scanner.nextInt();

                            // Ignore invalid tile IDs.
                            if (tileId >= 0 && tileId < tileCount) {

                                level.solidTileIds.add(tileId);
                            }
                        }

                        break;
                    }

                    default:

                        throw new IOException(
                                "Unknown map section: " + section
                        );
                }
            }

            return level;
        }
    }


    // ==========================================
    // SAVE MAP
    // ==========================================

    public void save(Path file) throws IOException {

        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder builder = new StringBuilder();

        // Map dimensions.
        builder.append(getColumns())
                .append(" ")
                .append(getRows())
                .append(" ")
                .append(tileSize)
                .append("\n");

        // Tile data.
        for (int row = 0; row < getRows(); row++) {

            for (int column = 0; column < getColumns(); column++) {

                builder.append(tiles[row][column]).append(" ");
            }

            builder.append("\n");
        }

        // Player spawn.
        builder.append("SPAWN ")
                .append(spawnColumn)
                .append(" ")
                .append(spawnRow)
                .append("\n");

        // Background.
        builder.append("BACKGROUND ")
                .append(backgroundName.isEmpty() ? "none" : backgroundName)
                .append("\n");

        // Enemies.
        builder.append("ENEMIES ")
                .append(enemySpawns.size())
                .append("\n");

        for (EnemySpawn enemy : enemySpawns) {

            builder.append(enemy.getType().name())
                    .append(" ")
                    .append(enemy.getColumn())
                    .append(" ")
                    .append(enemy.getRow())
                    .append("\n");
        }

        // Solid tile IDs.
        builder.append("SOLID_TILES ")
                .append(solidTileIds.size());

        for (int tileId : solidTileIds) {

            builder.append(" ").append(tileId);
        }

        builder.append("\n");

        Files.writeString(file, builder.toString());
    }


    // ==========================================
    // TILE ACCESS
    // ==========================================

    public int getTile(int column, int row) {

        return tiles[row][column];
    }

    public void setTile(int column, int row, int tileId) {

        if (column < 0 || column >= getColumns()
                || row < 0 || row >= getRows()) {

            return;
        }

        tiles[row][column] = tileId;
    }

    public int getTileSafe(int column, int row) {

        if (column < 0 || column >= getColumns()) {
            return -1;
        }

        if (row < 0) {
            return -1;
        }

        if (row >= getRows()) {
            return -1;
        }

        return tiles[row][column];
    }


    // ==========================================
    // SOLID TILE MANAGEMENT
    // ==========================================

    // Designates a tile type as solid or passable.
    public void setTileSolid(int tileId, boolean solid) {

        if (tileId < 0) {
            return;
        }

        if (solid) {

            solidTileIds.add(tileId);

        } else {

            solidTileIds.remove(tileId);
        }
    }

    // Checks whether a tile type is solid.
    public boolean isSolidTile(int tileId) {

        return solidTileIds.contains(tileId);
    }

    // Returns all designated solid tile IDs.
    public Set<Integer> getSolidTileIds() {

        return Collections.unmodifiableSet(solidTileIds);
    }

    // Checks whether a map position contains a solid tile.
    public boolean isSolidAt(int column, int row) {

        // Horizontal map boundaries are solid.
        if (column < 0 || column >= getColumns()) {
            return true;
        }

        // The area above the map is passable.
        if (row < 0) {
            return false;
        }

        // The bottom of the map acts as a floor.
        if (row >= getRows()) {
            return true;
        }

        int tileId = getTile(column, row);

        return isSolidTile(tileId);
    }


    // ==========================================
    // MAP DIMENSIONS
    // ==========================================

    public int getColumns() {

        return tiles[0].length;
    }

    public int getRows() {

        return tiles.length;
    }

    public int getTileSize() {

        return tileSize;
    }

    public int getWidth() {

        return getColumns() * tileSize;
    }

    public int getHeight() {

        return getRows() * tileSize;
    }


    // ==========================================
    // PLAYER SPAWN
    // ==========================================

    public void setSpawn(int column, int row) {

        spawnColumn = column;
        spawnRow = row;
    }

    public int getSpawnColumn() {

        return spawnColumn;
    }

    public int getSpawnRow() {

        return spawnRow;
    }


    // ==========================================
    // BACKGROUND
    // ==========================================

    public void setBackgroundName(String backgroundName) {

        this.backgroundName = backgroundName;
    }

    public String getBackgroundName() {

        return backgroundName;
    }


    // ==========================================
    // ENEMY SPAWNS
    // ==========================================

    public void addEnemySpawn(EnemySpawn spawn) {

        enemySpawns.add(spawn);
    }

    public void removeEnemySpawn(EnemySpawn spawn) {

        enemySpawns.remove(spawn);
    }

    public void removeEnemySpawnAt(int column, int row) {

        enemySpawns.removeIf(enemy ->
                enemy.getColumn() == column
                        && enemy.getRow() == row
        );
    }

    public List<EnemySpawn> getEnemySpawns() {

        return enemySpawns;
    }

    public void clearEnemySpawns() {

        enemySpawns.clear();
    }


    // ==========================================
    // EXPAND MAP
    // ==========================================

    public Level expandedTo(int newColumns, int newRows) {

        Level expanded = new Level(
                newColumns,
                newRows,
                tileSize
        );

        // Copy existing tile data.
        for (int row = 0; row < Math.min(getRows(), newRows); row++) {

            for (int column = 0;
                    column < Math.min(getColumns(), newColumns);
                    column++) {

                expanded.tiles[row][column] = tiles[row][column];
            }
        }

        // Copy player spawn.
        expanded.spawnColumn = spawnColumn;
        expanded.spawnRow = spawnRow;

        // Copy background.
        expanded.backgroundName = backgroundName;

        // Copy enemies.
        expanded.enemySpawns.addAll(enemySpawns);

        // Copy solid tile settings.
        expanded.solidTileIds.addAll(solidTileIds);

        return expanded;
    }


    // ==========================================
    // RESIZE TILE SIZE
    // ==========================================

    public Level resizedToTileSize(int newTileSize) {

        Level resized = new Level(
                getColumns(),
                getRows(),
                newTileSize
        );

        // Preserve tile layout.
        for (int row = 0; row < getRows(); row++) {

            for (int column = 0; column < getColumns(); column++) {

                resized.tiles[row][column] = tiles[row][column];
            }
        }

        // Preserve spawn position in world coordinates.
        resized.spawnColumn = spawnColumn * tileSize / newTileSize;
        resized.spawnRow = spawnRow * tileSize / newTileSize;

        // Preserve background.
        resized.backgroundName = backgroundName;

        // Preserve enemy positions in world coordinates.
        for (EnemySpawn enemy : enemySpawns) {

            int newColumn = enemy.getColumn() * tileSize / newTileSize;
            int newRow = enemy.getRow() * tileSize / newTileSize;

            resized.enemySpawns.add(
                    new EnemySpawn(
                            enemy.getType(),
                            newColumn,
                            newRow
                    )
            );
        }

        // Preserve solid tile settings.
        resized.solidTileIds.addAll(solidTileIds);

        return resized;
    }
}