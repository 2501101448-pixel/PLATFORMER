package levels;

import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class Level {

    public static final int DEFAULT_COLUMNS = 200;
    public static final int DEFAULT_ROWS = 24;
    public static final int DEFAULT_TILE_SIZE = 32;
    public static final String DEFAULT_BACKGROUND =
            "Background/Background.png";

    private final int[][] tiles;
    private final int tileSize;
    private int spawnColumn;
    private int spawnRow;
    private String backgroundName;
    private final List<EnemySpawn> enemySpawns = new ArrayList<>();

    public Level(int columns, int rows, int tileSize) {
        if (columns <= 0 || rows <= 0 || tileSize <= 0) {
            throw new IllegalArgumentException(
                    "Level dimensions and tile size must be positive."
            );
        }

        tiles = new int[rows][columns];
        this.tileSize = tileSize;
        spawnColumn = 1;
        spawnRow = 1;
        backgroundName = DEFAULT_BACKGROUND;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                tiles[row][column] = -1;
            }
        }
    }

    public static Level load(Path file, int tileCount) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            if (!scanner.hasNextInt()) {
                throw new IOException("The level header is missing.");
            }
            int columns = scanner.nextInt();
            if (!scanner.hasNextInt()) {
                throw new IOException("The level header is incomplete.");
            }
            int rows = scanner.nextInt();
            if (!scanner.hasNextInt()) {
                throw new IOException("The level header is incomplete.");
            }
            int tileSize = scanner.nextInt();

            Level level = new Level(columns, rows, tileSize);
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    if (!scanner.hasNextInt()) {
                        throw new IOException("The level map is incomplete.");
                    }

                    int tileId = scanner.nextInt();
                    if (tileId < -1 || tileId >= tileCount) {
                        throw new IOException(
                                "The level contains an unknown tile ID: "
                                + tileId
                        );
                    }
                    level.setTile(column, row, tileId);
                }
            }

            if (scanner.hasNext()) {
                String marker = scanner.next();
                if (!"SPAWN".equals(marker) || !scanner.hasNextInt()) {
                    throw new IOException(
                            "The spawn point in the level file is invalid."
                    );
                }
                int spawnColumn = scanner.nextInt();
                if (!scanner.hasNextInt()) {
                    throw new IOException(
                            "The spawn point in the level file is incomplete."
                    );
                }
                level.setSpawn(spawnColumn, scanner.nextInt());
            }

            if (scanner.hasNext()) {
                String marker = scanner.next();
                if (!"BACKGROUND".equals(marker) || !scanner.hasNext()) {
                    throw new IOException(
                            "The background setting in the level file is invalid."
                    );
                }
                level.setBackgroundName(scanner.next());
            }

            if (scanner.hasNext()) {
                String marker = scanner.next();
                if (!"ENEMIES".equals(marker) || !scanner.hasNextInt()) {
                    throw new IOException(
                            "The enemy list in the level file is invalid."
                    );
                }

                int enemyCount = scanner.nextInt();
                if (enemyCount < 0) {
                    throw new IOException(
                            "The enemy count cannot be negative."
                    );
                }
                for (int i = 0; i < enemyCount; i++) {
                    if (!scanner.hasNext()) {
                        throw new IOException(
                                "The enemy list is incomplete."
                        );
                    }

                    EnemyType type;
                    try {
                        type = EnemyType.valueOf(scanner.next());
                    } catch (IllegalArgumentException exception) {
                        throw new IOException(
                                "The level contains an unknown enemy type.",
                                exception
                        );
                    }

                    if (!scanner.hasNextInt()) {
                        throw new IOException(
                                "An enemy position is incomplete."
                        );
                    }
                    int column = scanner.nextInt();
                    if (!scanner.hasNextInt()) {
                        throw new IOException(
                                "An enemy position is incomplete."
                        );
                    }
                    level.setEnemy(type, column, scanner.nextInt());
                }
            }

            return level;
        } catch (IllegalArgumentException e) {
            throw new IOException("The level header is invalid.", e);
        }
    }

    public void save(Path file) throws IOException {
        Path folder = file.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(getColumns() + " " + getRows() + " " + tileSize);
            writer.newLine();
            for (int row = 0; row < getRows(); row++) {
                for (int column = 0; column < getColumns(); column++) {
                    if (column > 0) {
                        writer.write(" ");
                    }
                    writer.write(Integer.toString(tiles[row][column]));
                }
                writer.newLine();
            }
            writer.write("SPAWN " + spawnColumn + " " + spawnRow);
            writer.newLine();
            writer.write("BACKGROUND " + backgroundName);
            writer.newLine();
            writer.write("ENEMIES " + enemySpawns.size());
            writer.newLine();
            for (EnemySpawn enemy : enemySpawns) {
                writer.write(
                        enemy.getType().name() + " "
                        + enemy.getColumn() + " "
                        + enemy.getRow()
                );
                writer.newLine();
            }
        }
    }

    public int getColumns() {
        return tiles[0].length;
    }

    public int getRows() {
        return tiles.length;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getTile(int column, int row) {
        return tiles[row][column];
    }

    public void setTile(int column, int row, int tileId) {
        tiles[row][column] = tileId;
    }

    public Level expandedTo(int columns, int rows) {
        int newColumns = Math.max(columns, getColumns());
        int newRows = Math.max(rows, getRows());
        Level expandedLevel = new Level(newColumns, newRows, tileSize);

        for (int row = 0; row < getRows(); row++) {
            for (int column = 0; column < getColumns(); column++) {
                expandedLevel.setTile(column, row, getTile(column, row));
            }
        }

        expandedLevel.setSpawn(spawnColumn, spawnRow);
        expandedLevel.setBackgroundName(backgroundName);
        for (EnemySpawn enemy : enemySpawns) {
            expandedLevel.setEnemy(
                    enemy.getType(),
                    enemy.getColumn(),
                    enemy.getRow()
            );
        }
        return expandedLevel;
    }

    public Level resizedToTileSize(int newTileSize) {
        if (newTileSize <= 0 || tileSize % newTileSize != 0) {
            throw new IllegalArgumentException(
                    "New tile size must divide the current tile size."
            );
        }

        int scale = tileSize / newTileSize;
        if (scale == 1) {
            return this;
        }

        Level resizedLevel = new Level(
                getColumns() * scale,
                getRows() * scale,
                newTileSize
        );

        for (int row = 0; row < getRows(); row++) {
            for (int column = 0; column < getColumns(); column++) {
                int tileId = getTile(column, row);
                for (int y = 0; y < scale; y++) {
                    for (int x = 0; x < scale; x++) {
                        resizedLevel.setTile(
                                column * scale + x,
                                row * scale + y,
                                tileId
                        );
                    }
                }
            }
        }

        resizedLevel.setSpawn(
                spawnColumn * scale,
                spawnRow * scale
        );
        resizedLevel.setBackgroundName(backgroundName);
        for (EnemySpawn enemy : enemySpawns) {
            resizedLevel.setEnemy(
                    enemy.getType(),
                    enemy.getColumn() * scale,
                    enemy.getRow() * scale
            );
        }
        return resizedLevel;
    }

    public List<EnemySpawn> getEnemySpawns() {
        return Collections.unmodifiableList(enemySpawns);
    }

    public boolean setEnemy(EnemyType type, int column, int row) {
        if (type == null) {
            throw new IllegalArgumentException("Enemy type is required.");
        }
        if (column < 0 || column >= getColumns()
                || row < 0 || row >= getRows()) {
            throw new IllegalArgumentException(
                    "Enemy must be inside the level."
            );
        }

        for (EnemySpawn enemy : enemySpawns) {
            if (enemy.getColumn() == column && enemy.getRow() == row
                    && enemy.getType() == type) {
                return false;
            }
        }

        removeEnemy(column, row);
        enemySpawns.add(new EnemySpawn(type, column, row));
        return true;
    }

    public void clearEnemies() {
        enemySpawns.clear();
    }

    public boolean removeEnemy(int column, int row) {
        Iterator<EnemySpawn> enemies = enemySpawns.iterator();
        while (enemies.hasNext()) {
            EnemySpawn enemy = enemies.next();
            if (enemy.getColumn() == column && enemy.getRow() == row) {
                enemies.remove();
                return true;
            }
        }
        return false;
    }

    public String getBackgroundName() {
        return backgroundName;
    }

    public void setBackgroundName(String backgroundName) {
        if (backgroundName == null || backgroundName.trim().isEmpty()
                || backgroundName.contains(" ")
                || backgroundName.contains("\\")
                || backgroundName.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Background name must be a relative asset path."
            );
        }
        this.backgroundName = backgroundName;
    }

    public int getSpawnColumn() {
        return spawnColumn;
    }

    public int getSpawnRow() {
        return spawnRow;
    }

    public void setSpawn(int column, int row) {
        if (column < 0 || column >= getColumns()
                || row < 0 || row >= getRows()) {
            throw new IllegalArgumentException(
                    "Spawn point must be inside the level."
            );
        }
        spawnColumn = column;
        spawnRow = row;
    }
}
