package levels;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class BackgroundCatalog {

    private final List<BackgroundAsset> backgrounds = new ArrayList<>();

    private BackgroundCatalog() {
    }

    public static BackgroundCatalog load() throws IOException {
        BackgroundCatalog catalog = new BackgroundCatalog();
        Path assetRoot = findAssetRoot();
        catalog.addBackground(assetRoot, "Background/Background.png");
        catalog.addBackground(assetRoot, "Trees/Background.png");
        return catalog;
    }

    private static Path findAssetRoot() throws IOException {
        Path folderName = Paths.get("Legacy-Fantasy - High Forest 2.3");
        Path currentFolder = Paths.get("").toAbsolutePath();

        while (currentFolder != null) {
            Path assetRoot = currentFolder.resolve(folderName);
            if (Files.isDirectory(assetRoot)) {
                return assetRoot;
            }
            currentFolder = currentFolder.getParent();
        }

        throw new IOException(
                "Could not find the 'Legacy-Fantasy - High Forest 2.3' "
                + "asset folder."
        );
    }

    private void addBackground(Path assetRoot, String assetName)
            throws IOException {
        Path imagePath = assetRoot.resolve(assetName);
        if (!Files.isRegularFile(imagePath)) {
            throw new IOException("Background image is missing: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("Could not read background image: " + imagePath);
        }

        backgrounds.add(new BackgroundAsset(assetName, image));
    }

    public List<BackgroundAsset> getBackgrounds() {
        return new ArrayList<>(backgrounds);
    }

    public BackgroundAsset getBackground(String assetName) {
        for (BackgroundAsset background : backgrounds) {
            if (background.getAssetName().equals(assetName)) {
                return background;
            }
        }
        throw new IllegalArgumentException(
                "Unknown background asset: " + assetName
        );
    }

    public static class BackgroundAsset {

        private final String assetName;
        private final BufferedImage image;

        private BackgroundAsset(String assetName, BufferedImage image) {
            this.assetName = assetName;
            this.image = image;
        }

        public String getAssetName() {
            return assetName;
        }

        public BufferedImage getImage() {
            return image;
        }

        @Override
        public String toString() {
            return assetName;
        }
    }
}
