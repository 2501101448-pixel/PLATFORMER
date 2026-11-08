
package Main;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

import entities.Enemy;
import entities.MobAssets;
import entities.Player;
import levels.AssetCatalog;
import levels.AssetCatalog.TileAsset;
import levels.BackgroundCatalog;
import levels.BackgroundCatalog.BackgroundAsset;
import levels.EnemySpawn;
import levels.Level;
import levels.StageManager;

public class Game implements Runnable {

    private GameWindow gameWindow;
    private GamePanel gamePanel;
    private Thread gameThread;
    private AssetCatalog assets;
    private BackgroundCatalog backgrounds;
    private MobAssets mobAssets;
    private Level level;
    private final List<Enemy> enemies = new ArrayList<>();
    private int cameraX;
    private final int stageNumber;
    private int handledPlayerAttack;

    private final int FPS_SET = 120;
    private final int UPS_SET = 200;
    public static final int TILES_DEFAULT_SIZE = 32;
    public static final float SCALE = 1.5f;
    public static final int TILES_IN_WIDTH = 26;
    public static final int TILES_IN_HEIGHT = 14;
    public static final int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);
    public static final int GAME_WIDTH = 1280;
    public static final int GAME_HEIGHT = 800;
    
    private Player player;

    public Game() {
        this(1);
    }

    public Game(int stageNumber) {

        StageManager.getMapFile(stageNumber);
        this.stageNumber = stageNumber;
        loadLevel();
        initClasses();

        gamePanel = new GamePanel(this);
        gameWindow = new GameWindow(
                gamePanel,
                StageManager.getStageName(stageNumber)
        );

        gamePanel.requestFocusInWindow();

        startGameLoop();
    }

    private void initClasses() {

        int tileSize = level.getTileSize();
        player = new Player(0, 0);
        player.setWorldBounds(
                level.getColumns() * tileSize,
                level.getRows() * tileSize
        );
        player.setPosition(
                level.getSpawnColumn() * tileSize,
                level.getSpawnRow() * tileSize
        );
        for (EnemySpawn spawn : level.getEnemySpawns()) {
            enemies.add(new Enemy(spawn, tileSize, mobAssets));
        }
    }

    private void loadLevel() {
        try {
            assets = AssetCatalog.load();
            backgrounds = BackgroundCatalog.load();
            mobAssets = MobAssets.load();
            Path levelFile = StageManager.getMapFile(stageNumber);
            Level loadedLevel = Level.load(levelFile, assets.getTileCount());
            try {
                level = loadedLevel.resizedToTileSize(Level.DEFAULT_TILE_SIZE)
                        .expandedTo(
                                Level.DEFAULT_COLUMNS,
                                Level.DEFAULT_ROWS
                        );
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "The map tile size cannot be converted to "
                        + Level.DEFAULT_TILE_SIZE + " pixels.",
                        exception
                );
            }
            if (level.getColumns() != loadedLevel.getColumns()
                    || level.getRows() != loadedLevel.getRows()
                    || level.getTileSize() != loadedLevel.getTileSize()) {
                level.save(levelFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load "
                    + StageManager.getMapFile(stageNumber)
                    + " or its assets.",
                    exception
            );
        }
    }

    private void startGameLoop() {

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void update() {

        if (player != null) {
            player.update();
        }

        if (player.getAttackCount() != handledPlayerAttack) {
            handledPlayerAttack = player.getAttackCount();
            for (Enemy enemy : enemies) {
                enemy.hitByPlayer(player);
            }
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(player);
            if (enemy.isDefeated()) {
                enemies.remove(i);
            }
        }
    }

    public void render(Graphics2D g) {

        int worldWidth = level.getColumns() * level.getTileSize();
        int maxCameraX = Math.max(0, worldWidth - GAME_WIDTH);
        cameraX = (int) player.getPlayerX() - GAME_WIDTH / 2;
        cameraX = Math.max(0, Math.min(cameraX, maxCameraX));

        drawBackground(g);

        Graphics2D worldGraphics = (Graphics2D) g.create();
        worldGraphics.translate(-cameraX, 0);
        worldGraphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        int firstColumn = cameraX / level.getTileSize();
        int lastColumn = Math.min(
                level.getColumns() - 1,
                (cameraX + GAME_WIDTH) / level.getTileSize()
        );
        for (int row = 0; row < level.getRows(); row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                TileAsset tile = assets.getTile(level.getTile(column, row));
                if (tile == null) {
                    continue;
                }

                int x = column * level.getTileSize();
                int y = row * level.getTileSize();
                if (tile.getImage() != null) {
                    worldGraphics.drawImage(
                            tile.getImage(),
                            x,
                            y,
                            level.getTileSize(),
                            level.getTileSize(),
                            null
                    );
                } else {
                    worldGraphics.setColor(tile.getColor());
                    worldGraphics.fillRect(
                            x,
                            y,
                            level.getTileSize(),
                            level.getTileSize()
                    );
                }
            }
        }

        for (Enemy enemy : enemies) {
            enemy.draw(worldGraphics);
        }
        if (player != null) {
            player.draw(worldGraphics);
        }
        worldGraphics.dispose();
    }

    private void drawBackground(Graphics2D g) {
        BackgroundAsset background =
                backgrounds.getBackground(level.getBackgroundName());
        java.awt.image.BufferedImage image = background.getImage();
        int width = Math.max(
                1,
                image.getWidth() * GAME_HEIGHT / image.getHeight()
        );
        int cameraOffset = cameraX / 3;
        int firstX = -(cameraOffset % width);

        for (int x = firstX; x < GAME_WIDTH; x += width) {
            g.drawImage(image, x, 0, width, GAME_HEIGHT, null);
        }
    }

    @Override
    public void run() {

        double timePerFrame = 1000000000.0 / FPS_SET;
        double timePerUpdate = 1000000000.0 / UPS_SET;

        long previousTime = System.nanoTime();
        long lastCheck = System.currentTimeMillis();

        int frames = 0;
        int updates = 0;

        double deltaU = 0;
        double deltaF = 0;

        while (true) {

            long currentTime = System.nanoTime();

            deltaU += (currentTime - previousTime) / timePerUpdate;
            deltaF += (currentTime - previousTime) / timePerFrame;

            previousTime = currentTime;

            if (deltaU >= 1) {
                update();
                updates++;
                deltaU--;
            }

            if (deltaF >= 1) {
                gamePanel.repaint();
                frames++;
                deltaF--;
            }

            if (System.currentTimeMillis() - lastCheck >= 1000) {

                lastCheck = System.currentTimeMillis();

                System.out.println(
                    "FPS: " + frames + " | UPS: " + updates
                );

                frames = 0;
                updates = 0;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public Level getLevel() {
        return level;
    }
}
