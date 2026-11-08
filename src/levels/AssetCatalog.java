package levels;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class AssetCatalog {

    private static final int SPRITE_SIZE = 32;

    private final List<TileAsset> tiles = new ArrayList<>();
    private final Map<String, List<TileAsset>> tilesByCategory =
            new LinkedHashMap<>();

    private AssetCatalog() {
        addBasicTiles();
    }

    public static AssetCatalog load() throws IOException {
        AssetCatalog catalog = new AssetCatalog();
        Path assetRoot = findAssetRoot();

        List<Path> images = new ArrayList<>();
        collectPngFiles(assetRoot.resolve("Assets"), images);
        collectPngFiles(assetRoot.resolve("Trees"), images);
        Collections.sort(images);

        for (Path imagePath : images) {
            catalog.addSpritesFromImage(assetRoot, imagePath);
        }

        return catalog;
    }

    private static Path findAssetRoot() throws IOException {
        Path folderName = Paths.get("Legacy-Fantasy - High Forest 2.3");
        Path currentFolder = Paths.get("").toAbsolutePath();

        while (currentFolder != null) {
            Path assetRoot = currentFolder.resolve(folderName);
            if (Files.isDirectory(assetRoot.resolve("Assets"))
                    && Files.isDirectory(assetRoot.resolve("Trees"))) {
                return assetRoot;
            }
            currentFolder = currentFolder.getParent();
        }

        throw new IOException(
                "Could not find the 'Legacy-Fantasy - High Forest 2.3' "
                + "asset folder."
        );
    }

    private static void collectPngFiles(Path folder, List<Path> images)
            throws IOException {
        if (!Files.isDirectory(folder)) {
            return;
        }

        File[] files = folder.toFile().listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectPngFiles(file.toPath(), images);
            } else if (file.getName().toLowerCase().endsWith(".png")
                    && !file.getName().equalsIgnoreCase("Background.png")) {
                images.add(file.toPath());
            }
        }
    }

    private void addBasicTiles() {
        Color[] colors = {
            new Color(70, 170, 70),
            new Color(130, 85, 45),
            Color.GRAY,
            new Color(40, 120, 220),
            new Color(150, 100, 50)
        };
        String[] names = {"Grass", "Dirt", "Stone", "Water", "Wood"};

        for (int i = 0; i < names.length; i++) {
            addTile(new TileAsset(
                    tiles.size(),
                    "Basic",
                    names[i],
                    null,
                    colors[i]
            ));
        }
    }

    private void addSpritesFromImage(Path assetRoot, Path imagePath)
            throws IOException {
        BufferedImage sheet = ImageIO.read(imagePath.toFile());
        if (sheet == null) {
            return;
        }

        String category = assetRoot.relativize(imagePath)
                .toString().replace(File.separatorChar, '/');

        int rows = sheet.getHeight() / SPRITE_SIZE;
        int columns = sheet.getWidth() / SPRITE_SIZE;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BufferedImage sprite = sheet.getSubimage(
                        column * SPRITE_SIZE,
                        row * SPRITE_SIZE,
                        SPRITE_SIZE,
                        SPRITE_SIZE
                );

                if (hasVisiblePixel(sprite)) {
                    String name = imagePath.getFileName()
                            + " (" + column + ", " + row + ")";
                    addTile(new TileAsset(
                            tiles.size(),
                            category,
                            name,
                            sprite,
                            null
                    ));
                }
            }
        }
    }

    private boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addTile(TileAsset tile) {
        tiles.add(tile);
        List<TileAsset> categoryTiles = tilesByCategory.get(tile.getCategory());
        if (categoryTiles == null) {
            categoryTiles = new ArrayList<>();
            tilesByCategory.put(tile.getCategory(), categoryTiles);
        }
        categoryTiles.add(tile);
    }

    public int getTileCount() {
        return tiles.size();
    }

    public TileAsset getTile(int tileId) {
        if (tileId < 0 || tileId >= tiles.size()) {
            return null;
        }
        return tiles.get(tileId);
    }

    public List<String> getCategories() {
        return new ArrayList<>(tilesByCategory.keySet());
    }

    public List<TileAsset> getTiles(String category) {
        List<TileAsset> categoryTiles = tilesByCategory.get(category);
        return categoryTiles == null
                ? new ArrayList<>()
                : new ArrayList<>(categoryTiles);
    }

    public static class TileAsset {

        private final int id;
        private final String category;
        private final String name;
        private final BufferedImage image;
        private final Color color;

        private TileAsset(
                int id,
                String category,
                String name,
                BufferedImage image,
                Color color
        ) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.image = image;
            this.color = color;
        }

        public int getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        public String getName() {
            return name;
        }

        public BufferedImage getImage() {
            return image;
        }

        public Color getColor() {
            return color;
        }
    }
}
