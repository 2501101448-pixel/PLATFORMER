package LevelIditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import entities.Enemy;
import entities.MobAssets;
import entities.Player;
import levels.AssetCatalog;
import levels.AssetCatalog.TileAsset;
import levels.BackgroundCatalog;
import levels.BackgroundCatalog.BackgroundAsset;
import levels.EnemySpawn;
import levels.EnemyType;
import levels.Level;
import levels.StageManager;

public class LevelEditor extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int COLUMNS = Level.DEFAULT_COLUMNS;
    private static final int ROWS = Level.DEFAULT_ROWS;
    private static final int TILE_SIZE = Level.DEFAULT_TILE_SIZE;
    private final AssetCatalog assets;
    private final BackgroundCatalog backgrounds;
    private final MobAssets mobAssets;
    private final Player playerPreview;
    private Level level;
    private int currentStage = 1;
    private JComboBox<String> stageSelector;
    private JComboBox<BackgroundAsset> backgroundSelector;
    private final DefaultListModel<TileAsset> paletteItems =
            new DefaultListModel<>();
    private final JLabel statusLabel = new JLabel("Choose a resource to paint.");
    private final EditorPanel editorPanel = new EditorPanel();
    private final Timer saveTimer = new Timer(500, event -> saveLevel());
    private final Timer previewTimer =
            new Timer(20, event -> editorPanel.repaint());

    private int selectedTile = 0;
    private boolean settingSpawn;
    private boolean settingEnemy;
    private JToggleButton spawnButton;
    private JToggleButton enemyButton;
    private JComboBox<EnemyType> enemySelector;

    public LevelEditor() throws IOException {
        assets = AssetCatalog.load();
        backgrounds = BackgroundCatalog.load();
        mobAssets = MobAssets.load();
        playerPreview = new Player(0, 0);
        playerPreview.setWorldBounds(COLUMNS * TILE_SIZE, ROWS * TILE_SIZE);
        level = loadStage(currentStage);
        saveTimer.setRepeats(false);
        previewTimer.start();

        setTitle("Platformer Level Editor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        createToolbar();
        createPalette();
        createCanvas();

        setSize(1250, 850);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveTimer.stop();
                previewTimer.stop();
                saveLevel();
                dispose();
            }
        });
        editorPanel.repaint();
    }

    private Level loadStage(int stageNumber) throws IOException {
        Path mapFile = StageManager.getMapFile(stageNumber);
        if (java.nio.file.Files.exists(mapFile)) {
            Level loadedLevel = Level.load(mapFile, assets.getTileCount());
            try {
                backgrounds.getBackground(loadedLevel.getBackgroundName());
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "The stage uses an unknown background.",
                        exception
                );
            }
            Level expandedLevel;
            try {
                expandedLevel = loadedLevel.resizedToTileSize(TILE_SIZE)
                        .expandedTo(COLUMNS, ROWS);
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "The map tile size cannot be converted to "
                        + TILE_SIZE + " pixels.",
                        exception
                );
            }
            if (expandedLevel.getColumns() != loadedLevel.getColumns()
                    || expandedLevel.getRows() != loadedLevel.getRows()
                    || expandedLevel.getTileSize() != loadedLevel.getTileSize()) {
                expandedLevel.save(mapFile);
            }
            return expandedLevel;
        }

        Level newLevel = new Level(COLUMNS, ROWS, TILE_SIZE);
        newLevel.save(mapFile);
        return newLevel;
    }

    private void createToolbar() {
        JPanel toolbar = new JPanel(new java.awt.GridLayout(3, 1));
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel assetRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel enemyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveButton = new JButton("Save");
        JButton playButton = new JButton("Play");
        JButton loadButton = new JButton("Reload");
        JButton clearButton = new JButton("Clear");
        stageSelector = new JComboBox<>();
        for (int stage = 1; stage <= StageManager.STAGE_COUNT; stage++) {
            stageSelector.addItem(StageManager.getStageName(stage));
        }
        stageSelector.setSelectedIndex(currentStage - 1);
        stageSelector.addActionListener(event ->
                changeStage(stageSelector.getSelectedIndex() + 1)
        );
        backgroundSelector = new JComboBox<>(
                backgrounds.getBackgrounds().toArray(new BackgroundAsset[0])
        );
        backgroundSelector.setPreferredSize(new Dimension(220, 28));
        backgroundSelector.setSelectedItem(
                backgrounds.getBackground(level.getBackgroundName())
        );
        backgroundSelector.addActionListener(event -> {
            BackgroundAsset selectedBackground =
                    (BackgroundAsset) backgroundSelector.getSelectedItem();
            if (selectedBackground != null
                    && !selectedBackground.getAssetName().equals(
                            level.getBackgroundName())) {
                level.setBackgroundName(selectedBackground.getAssetName());
                editorPanel.repaint();
                markChanged();
            }
        });
        spawnButton = new JToggleButton("Set Player Spawn");
        enemyButton = new JToggleButton("Place Enemy");
        enemySelector = new JComboBox<>(EnemyType.values());

        saveButton.addActionListener(event -> {
            saveTimer.stop();
            saveLevel();
        });
        playButton.addActionListener(event -> {
            saveTimer.stop();
            if (saveLevel()) {
                SwingUtilities.invokeLater(() -> new Main.Game(currentStage));
            }
        });
        loadButton.addActionListener(event -> reloadLevel());
        clearButton.addActionListener(event -> clearLevel());
        spawnButton.addActionListener(event -> {
            settingSpawn = spawnButton.isSelected();
            settingEnemy = false;
            enemyButton.setSelected(false);
            statusLabel.setText(settingSpawn
                    ? "Click a map cell to place the player spawn."
                    : "Choose a resource to paint.");
        });
        enemyButton.addActionListener(event -> {
            settingEnemy = enemyButton.isSelected();
            settingSpawn = false;
            spawnButton.setSelected(false);
            statusLabel.setText(settingEnemy
                    ? "Click a map cell to place an enemy."
                    : "Choose a resource to paint.");
        });

        topRow.add(saveButton);
        topRow.add(playButton);
        topRow.add(loadButton);
        topRow.add(clearButton);
        topRow.add(new JLabel("Stage:"));
        topRow.add(stageSelector);

        assetRow.add(new JLabel("Background:"));
        assetRow.add(backgroundSelector);
        assetRow.add(spawnButton);
        assetRow.add(new JLabel("Left click: place | Right click: erase"));
        assetRow.add(new JLabel("Map: " + COLUMNS + " x " + ROWS
                + " tiles, " + TILE_SIZE + " x " + TILE_SIZE
                + " pixels per tile (scroll sideways)"));

        enemyRow.add(enemyButton);
        enemyRow.add(new JLabel("Enemy:"));
        enemyRow.add(enemySelector);
        toolbar.add(topRow);
        toolbar.add(assetRow);
        toolbar.add(enemyRow);
        add(toolbar, BorderLayout.NORTH);
    }

    private void createPalette() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("RESOURCES");
        title.setFont(new Font("Arial", Font.BOLD, 17));

        JComboBox<String> categories = new JComboBox<>(
                assets.getCategories().toArray(new String[0])
        );
        JPanel assetPicker = new JPanel(new BorderLayout(6, 6));
        JList<TileAsset> tileList = new JList<>(paletteItems);
        tileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tileList.setCellRenderer((list, tile, index, selected, focused) -> {
            JLabel item = new JLabel(tile.getName(), createTileIcon(tile), 2);
            item.setOpaque(true);
            item.setBackground(selected ? new Color(200, 220, 255) : Color.WHITE);
            item.setBorder(new EmptyBorder(3, 4, 3, 4));
            return item;
        });
        tileList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && tileList.getSelectedValue() != null) {
                selectedTile = tileList.getSelectedValue().getId();
                settingSpawn = false;
                settingEnemy = false;
                spawnButton.setSelected(false);
                enemyButton.setSelected(false);
                statusLabel.setText(
                        "Selected: " + tileList.getSelectedValue().getName()
                );
            }
        });
        categories.addActionListener(event ->
                showCategory(tileList, (String) categories.getSelectedItem())
        );

        assetPicker.add(categories, BorderLayout.NORTH);
        assetPicker.add(new JScrollPane(tileList), BorderLayout.CENTER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(assetPicker, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(255, 0));
        add(panel, BorderLayout.WEST);

        showCategory(tileList, (String) categories.getSelectedItem());
    }

    private void showCategory(JList<TileAsset> tileList, String category) {
        paletteItems.clear();
        if (category == null) {
            return;
        }
        for (TileAsset tile : assets.getTiles(category)) {
            paletteItems.addElement(tile);
        }
        if (!paletteItems.isEmpty()) {
            tileList.setSelectedIndex(0);
        }
    }

    private ImageIcon createTileIcon(TileAsset tile) {
        BufferedImage icon = new BufferedImage(
                40,
                40,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = icon.createGraphics();
        if (tile.getImage() != null) {
            graphics.drawImage(
                    tile.getImage(),
                    0,
                    0,
                    icon.getWidth(),
                    icon.getHeight(),
                    null
            );
        } else {
            graphics.setColor(tile.getColor());
            graphics.fillRect(0, 0, icon.getWidth(), icon.getHeight());
        }
        graphics.dispose();
        return new ImageIcon(icon);
    }

    private void createCanvas() {
        editorPanel.setPreferredSize(new Dimension(
                level.getColumns() * TILE_SIZE,
                level.getRows() * TILE_SIZE
        ));
        JScrollPane mapScroll = new JScrollPane(editorPanel);
        mapScroll.getVerticalScrollBar().setUnitIncrement(32);
        mapScroll.getHorizontalScrollBar().setUnitIncrement(32);
        add(mapScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(statusLabel);
        add(footer, BorderLayout.SOUTH);
    }

    private boolean saveLevel() {
        try {
            Path mapFile = StageManager.getMapFile(currentStage);
            level.save(mapFile);
            statusLabel.setText("Saved " + mapFile + ".");
            return true;
        } catch (IOException exception) {
            showError("Unable to save the level:\n" + exception.getMessage());
            return false;
        }
    }

    private void reloadLevel() {
        saveTimer.stop();
        try {
            level = loadStage(currentStage);
            backgroundSelector.setSelectedItem(
                    backgrounds.getBackground(level.getBackgroundName())
            );
            editorPanel.setPreferredSize(new Dimension(
                    level.getColumns() * TILE_SIZE,
                    level.getRows() * TILE_SIZE
            ));
            editorPanel.revalidate();
            editorPanel.repaint();
            statusLabel.setText("Level reloaded.");
        } catch (IOException exception) {
            showError("Unable to reload the level:\n" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("Unable to resize the level:\n" + exception.getMessage());
        }
    }

    private void changeStage(int stageNumber) {
        if (stageNumber == currentStage) {
            return;
        }

        saveTimer.stop();
        if (!saveLevel()) {
            stageSelector.setSelectedIndex(currentStage - 1);
            return;
        }

        try {
            level = loadStage(stageNumber);
            currentStage = stageNumber;
            backgroundSelector.setSelectedItem(
                    backgrounds.getBackground(level.getBackgroundName())
            );
            editorPanel.setPreferredSize(new Dimension(
                    level.getColumns() * TILE_SIZE,
                    level.getRows() * TILE_SIZE
            ));
            editorPanel.revalidate();
            editorPanel.repaint();
            statusLabel.setText(
                    StageManager.getStageName(currentStage) + " loaded."
            );
        } catch (IOException exception) {
            stageSelector.setSelectedIndex(currentStage - 1);
            showError("Unable to open the stage:\n" + exception.getMessage());
        }
    }

    private void clearLevel() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Clear every tile and enemy in this level?",
                "Clear Level",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        for (int row = 0; row < level.getRows(); row++) {
            for (int column = 0; column < level.getColumns(); column++) {
                level.setTile(column, row, -1);
            }
        }
        level.clearEnemies();
        editorPanel.repaint();
        saveLevel();
    }

    private void showError(String message) {
        statusLabel.setText("Level operation failed.");
        JOptionPane.showMessageDialog(
                this,
                message,
                "Level Editor Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void markChanged() {
        statusLabel.setText("Unsaved changes...");
        saveTimer.restart();
    }

    private class EditorPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private EditMode editMode = EditMode.NONE;

        private enum EditMode {
            NONE,
            PLACE_TILE,
            ERASE_TILE,
            SET_SPAWN,
            PLACE_ENEMY,
            ERASE_ENEMY
        }

        private EditorPanel() {
            setBackground(new Color(45, 45, 45));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event)
                            && !SwingUtilities.isRightMouseButton(event)) {
                        return;
                    }
                    if (settingSpawn) {
                        editMode = EditMode.SET_SPAWN;
                    } else if (settingEnemy) {
                        editMode = SwingUtilities.isRightMouseButton(event)
                                ? EditMode.ERASE_ENEMY
                                : EditMode.PLACE_ENEMY;
                    } else {
                        editMode = SwingUtilities.isRightMouseButton(event)
                                ? EditMode.ERASE_TILE
                                : EditMode.PLACE_TILE;
                    }
                    editMap(event);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    editMode = EditMode.NONE;
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    if (editMode != EditMode.NONE) {
                        editMap(event);
                    }
                }
            });
        }

        private void editMap(MouseEvent event) {
            int column = event.getX() / TILE_SIZE;
            int row = event.getY() / TILE_SIZE;
            if (column < 0 || column >= level.getColumns()
                    || row < 0 || row >= level.getRows()) {
                return;
            }

            if (editMode == EditMode.SET_SPAWN) {
                level.setSpawn(column, row);
                settingSpawn = false;
                spawnButton.setSelected(false);
                editMode = EditMode.NONE;
                repaint();
                markChanged();
                return;
            }

            if (editMode == EditMode.PLACE_ENEMY) {
                EnemyType type = (EnemyType) enemySelector.getSelectedItem();
                if (type != null
                        && level.setEnemy(type, column, row)) {
                    repaint();
                    markChanged();
                }
                return;
            }

            if (editMode == EditMode.ERASE_ENEMY) {
                if (level.removeEnemy(column, row)) {
                    repaint();
                    markChanged();
                }
                return;
            }

            int newTile;
            if (editMode == EditMode.ERASE_TILE) {
                newTile = -1;
            } else if (editMode == EditMode.PLACE_TILE) {
                newTile = selectedTile;
            } else {
                return;
            }
            if (level.getTile(column, row) != newTile) {
                level.setTile(column, row, newTile);
                repaint();
                markChanged();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
            drawBackground(g);

            for (int row = 0; row < level.getRows(); row++) {
                for (int column = 0; column < level.getColumns(); column++) {
                    int x = column * TILE_SIZE;
                    int y = row * TILE_SIZE;
                    drawTile(g, level.getTile(column, row), x, y);

                    g.setColor(new Color(0, 0, 0, 100));
                    g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                }
            }

            drawEnemies(g);
            drawPlayerSpawn(g);
            g.dispose();
        }

        private void drawBackground(Graphics2D g) {
            BufferedImage image = backgrounds
                    .getBackground(level.getBackgroundName())
                    .getImage();
            int height = level.getRows() * TILE_SIZE;
            int width = Math.max(
                    1,
                    image.getWidth() * height / image.getHeight()
            );

            for (int x = 0; x < getWidth(); x += width) {
                g.drawImage(image, x, 0, width, height, null);
            }
        }

        private void drawTile(Graphics2D g, int tileId, int x, int y) {
            TileAsset tile = assets.getTile(tileId);
            if (tile == null) {
                return;
            }
            if (tile.getImage() != null) {
                g.drawImage(tile.getImage(), x, y, TILE_SIZE, TILE_SIZE, null);
            } else {
                g.setColor(tile.getColor());
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        private void drawPlayerSpawn(Graphics2D g) {
            int x = level.getSpawnColumn() * TILE_SIZE;
            int y = level.getSpawnRow() * TILE_SIZE;
            playerPreview.setPosition(x, y);
            playerPreview.draw(g);
        }

        private void drawEnemies(Graphics2D g) {
            for (EnemySpawn spawn : level.getEnemySpawns()) {
                Enemy enemy = new Enemy(spawn, TILE_SIZE, mobAssets);
                enemy.drawPreview(g);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LevelEditor().setVisible(true);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Unable to open the editor:\n" + exception.getMessage(),
                        "Level Editor Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
