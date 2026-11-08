
package entities;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import levels.EnemyType;

public class MobAssets {

    // ==========================================
    // ANIMATION TYPES
    // ==========================================

    public enum Animation {

        IDLE,
        WALK,
        RUN,
        ATTACK,
        HIT,
        HIDE,
        DEAD,
        FLY
    }

    // ==========================================
    // ANIMATION STORAGE
    // ==========================================

    private final Map<EnemyType, Map<Animation, BufferedImage[]>> animations =
            new EnumMap<>(EnemyType.class);

    private MobAssets() {

        for (EnemyType type : EnemyType.values()) {

            animations.put(
                    type,
                    new EnumMap<>(Animation.class)
            );
        }
    }

    // ==========================================
    // LOAD ALL MOB ASSETS
    // ==========================================

    public static MobAssets load() throws IOException {

        MobAssets assets = new MobAssets();

        Path mobFolder = findMobFolder();

        // ======================================
        // BOAR
        // ======================================

        assets.loadSheet(
                EnemyType.BOAR,
                Animation.IDLE,
                mobFolder.resolve(Paths.get(
                        "Boar", "Idle", "Idle-Sheet.png"
                )),
                32,
                32
        );

        assets.loadSheet(
                EnemyType.BOAR,
                Animation.WALK,
                mobFolder.resolve(Paths.get(
                        "Boar", "Walk", "Walk-Base-Sheet.png"
                )),
                32,
                32
        );

        assets.loadSheet(
                EnemyType.BOAR,
                Animation.RUN,
                mobFolder.resolve(Paths.get(
                        "Boar", "Run", "Run-Sheet.png"
                )),
                32,
                32
        );

        assets.loadSheet(
                EnemyType.BOAR,
                Animation.HIT,
                mobFolder.resolve(Paths.get(
                        "Boar", "Hit-Vanish", "Hit-Sheet.png"
                )),
                32,
                32
        );

        // ======================================
        // SNAIL
        // ======================================

        assets.loadSheet(
                EnemyType.SNAIL,
                Animation.WALK,
                mobFolder.resolve(Paths.get(
                        "Snail", "walk-Sheet.png"
                )),
                32,
                32
        );

        assets.loadSheet(
                EnemyType.SNAIL,
                Animation.HIDE,
                mobFolder.resolve(Paths.get(
                        "Snail", "Hide-Sheet.png"
                )),
                32,
                32
        );

        assets.loadSheet(
                EnemyType.SNAIL,
                Animation.DEAD,
                mobFolder.resolve(Paths.get(
                        "Snail", "Dead-Sheet.png"
                )),
                32,
                32
        );

        // ======================================
        // SMALL BEE
        // ======================================

        assets.loadSheet(
                EnemyType.SMALL_BEE,
                Animation.ATTACK,
                mobFolder.resolve(Paths.get(
                        "Small Bee", "Attack", "Attack-Sheet.png"
                )),
                64,
                64
        );

        assets.loadSheet(
                EnemyType.SMALL_BEE,
                Animation.FLY,
                mobFolder.resolve(Paths.get(
                        "Small Bee", "Fly", "Fly-Sheet.png"
                )),
                64,
                64
        );

        assets.loadSheet(
                EnemyType.SMALL_BEE,
                Animation.HIT,
                mobFolder.resolve(Paths.get(
                        "Small Bee", "Hit", "Hit-Sheet.png"
                )),
                64,
                64
        );

        return assets;
    }

    // ==========================================
    // FIND MOB FOLDER
    // ==========================================

    private static Path findMobFolder() throws IOException {

        Path folderName = Paths.get(
                "Legacy-Fantasy - High Forest 2.3",
                "Mob"
        );

        Path currentFolder = Paths.get("")
                .toAbsolutePath()
                .normalize();

        while (currentFolder != null) {

            Path mobFolder = currentFolder
                    .resolve(folderName)
                    .normalize();

            if (Files.isDirectory(mobFolder)) {

                return mobFolder;
            }

            currentFolder = currentFolder.getParent();
        }

        throw new IOException(
                "Could not find Mob folder. " +
                "Check your Legacy-Fantasy asset folder."
        );
    }

    // ==========================================
    // LOAD SPRITE SHEET
    // ==========================================

    private void loadSheet(
            EnemyType type,
            Animation animation,
            Path file,
            int frameWidth,
            int frameHeight
    ) throws IOException {

        if (!Files.exists(file)) {

            throw new IOException(
                    "Missing animation file: " + file.toAbsolutePath()
            );
        }

        BufferedImage sheet = ImageIO.read(file.toFile());

        if (sheet == null) {

            throw new IOException(
                    "Cannot read animation sheet: " + file
            );
        }

        if (sheet.getWidth() < frameWidth ||
                sheet.getHeight() < frameHeight ||
                sheet.getWidth() % frameWidth != 0 ||
                sheet.getHeight() % frameHeight != 0) {

            throw new IOException(
                    "Invalid sprite sheet dimensions: " +
                    file +
                    " (" + sheet.getWidth() +
                    "x" + sheet.getHeight() + ")"
            );
        }

        int columns = sheet.getWidth() / frameWidth;
        int rows = sheet.getHeight() / frameHeight;

        int frameCount = columns * rows;

        BufferedImage[] frames =
                new BufferedImage[frameCount];

        int index = 0;

        for (int row = 0; row < rows; row++) {

            for (int column = 0; column < columns; column++) {

                frames[index] = sheet.getSubimage(
                        column * frameWidth,
                        row * frameHeight,
                        frameWidth,
                        frameHeight
                );

                index++;
            }
        }

        animations.get(type).put(animation, frames);

        System.out.println(
                "Loaded " + type +
                " - " + animation +
                " (" + frameCount + " frames)"
        );
    }

    // ==========================================
    // CHECK ANIMATION
    // ==========================================

    public boolean hasAnimation(
            EnemyType type,
            Animation animation
    ) {

        Map<Animation, BufferedImage[]> mobAnimations =
                animations.get(type);

        return mobAnimations != null &&
                mobAnimations.containsKey(animation);
    }

    // ==========================================
    // GET FRAME COUNT
    // ==========================================

    public int getFrameCount(
            EnemyType type,
            Animation animation
    ) {

        Map<Animation, BufferedImage[]> mobAnimations =
                animations.get(type);

        if (mobAnimations == null) {
            return 0;
        }

        BufferedImage[] frames = mobAnimations.get(animation);

        return frames == null ? 0 : frames.length;
    }

    // ==========================================
    // GET ANIMATION FRAME
    // ==========================================

    public BufferedImage getFrame(
            EnemyType type,
            Animation animation,
            int frame
    ) {

        Map<Animation, BufferedImage[]> mobAnimations =
                animations.get(type);

        if (mobAnimations == null) {
            return null;
        }

        BufferedImage[] frames = mobAnimations.get(animation);

        if (frames == null || frames.length == 0) {
            return null;
        }

        int safeFrame = Math.floorMod(frame, frames.length);

        return frames[safeFrame];
    }

    // ==========================================
    // PREVIEW ANIMATION
    // ==========================================

    public Animation getPreviewAnimation(EnemyType type) {

        switch (type) {

            case BOAR:

                return Animation.IDLE;

            case SNAIL:

                return Animation.WALK;

            case SMALL_BEE:

                return Animation.FLY;

            default:

                throw new IllegalArgumentException(
                        "Unknown enemy type: " + type
                );
        }
    }

    // ==========================================
    // ATTACK ANIMATION
    // ==========================================

    public Animation getAttackAnimation(EnemyType type) {

        if (hasAnimation(type, Animation.ATTACK)) {

            return Animation.ATTACK;
        }

        if (type == EnemyType.SNAIL &&
                hasAnimation(type, Animation.HIDE)) {

            return Animation.HIDE;
        }

        if (type == EnemyType.BOAR &&
                hasAnimation(type, Animation.RUN)) {

            return Animation.RUN;
        }

        return getPreviewAnimation(type);
    }
}
