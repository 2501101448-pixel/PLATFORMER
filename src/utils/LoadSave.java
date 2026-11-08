package utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import java.awt.image.RasterFormatException;

import static utils.Constants.PlayerConstants.GetSpriteAmount;

public class LoadSave {

    private static final int FRAME_WIDTH = 64;
    private static final int FRAME_HEIGHT = 64;
    private static final int ACTION_COUNT = 15;

    private static final String[] SPRITE_PATHS = {
        "/male_spritesheet.png",
        "/textures/male_spritesheet.png",
        "/player_sprites.png",
        "/textures/player_sprites.png"
    };

    private LoadSave() {
        // Utility class
    }

    public static BufferedImage[][] GetAnimations() {

        BufferedImage[][] animations =
                new BufferedImage[ACTION_COUNT][];

        BufferedImage spriteSheet = loadSpriteSheet();

        if (spriteSheet == null) {
            System.err.println(
                    "ERROR: Could not load the player sprite sheet."
            );
            return animations;
        }

        System.out.println(
                "Sprite sheet loaded: "
                + spriteSheet.getWidth() + " x "
                + spriteSheet.getHeight()
        );

        int availableRows = spriteSheet.getHeight() / FRAME_HEIGHT;
        int availableColumns = spriteSheet.getWidth() / FRAME_WIDTH;

        for (int action = 0; action < ACTION_COUNT; action++) {

            int requestedFrames = GetSpriteAmount(action);

            if (requestedFrames <= 0 || action >= availableRows) {
                animations[action] = new BufferedImage[0];
                continue;
            }

            int validFrames = Math.min(requestedFrames, availableColumns);

            animations[action] = new BufferedImage[validFrames];

            for (int frame = 0; frame < validFrames; frame++) {

                int spriteX = frame * FRAME_WIDTH;
                int spriteY = action * FRAME_HEIGHT;

                try {
                    animations[action][frame] =
                            spriteSheet.getSubimage(
                                    spriteX,
                                    spriteY,
                                    FRAME_WIDTH,
                                    FRAME_HEIGHT
                            );

                } catch (RasterFormatException e) {
                    System.err.println(
                            "Could not extract action " + action
                            + ", frame " + frame
                    );
                }
            }

            System.out.println(
                    "Action " + action + ": "
                    + countValidFrames(animations[action])
                    + " frames loaded."
            );
        }

        return animations;
    }

    private static BufferedImage loadSpriteSheet() {

        for (String path : SPRITE_PATHS) {

            try (InputStream stream =
                    LoadSave.class.getResourceAsStream(path)) {

                if (stream == null) {
                    continue;
                }

                BufferedImage image = ImageIO.read(stream);

                if (image != null) {
                    System.out.println("Using sprite path: " + path);
                    return image;
                }

            } catch (IOException e) {
                System.err.println(
                        "Error reading sprite sheet at " + path
                );
                e.printStackTrace();
            }
        }

        System.err.println("Checked these sprite paths:");

        for (String path : SPRITE_PATHS) {
            System.err.println(path);
        }

        return null;
    }

    private static int countValidFrames(BufferedImage[] frames) {

        int count = 0;

        if (frames == null) {
            return 0;
        }

        for (BufferedImage frame : frames) {
            if (frame != null) {
                count++;
            }
        }

        return count;
    }
}