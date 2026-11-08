
package systems;

import entities.Player;
import levels.Level;

public class Camera {

    private float x = 0;
    private float y = 0;

    private static final float ZOOM = 2.3f;
    private static final float FOLLOW_SPEED = 0.05f;

    private final int SCREEN_WIDTH;
    private final int SCREEN_HEIGHT;

    private boolean initialized = false;

    public Camera(int screenWidth, int screenHeight) {
        this.SCREEN_WIDTH = screenWidth;
        this.SCREEN_HEIGHT = screenHeight;
    }

    public void update(Player player, Level level) {

        // Calculate visible world dimensions.
        float visibleWidth = SCREEN_WIDTH / ZOOM;
        float visibleHeight = SCREEN_HEIGHT / ZOOM;

        // Get the player's center position.
        float playerCenterX = player.getPlayerX();
        float playerCenterY = player.getPlayerY();

        // Calculate camera target position.
        float targetX = playerCenterX - visibleWidth / 2f;
        float targetY = playerCenterY - visibleHeight / 2f;

        // Calculate level boundaries.
        float worldWidth =
                level.getColumns() * level.getTileSize();

        float worldHeight =
                level.getRows() * level.getTileSize();

        float maxCameraX = Math.max(
                0,
                worldWidth - visibleWidth
        );

        float maxCameraY = Math.max(
                0,
                worldHeight - visibleHeight
        );

        // Keep camera inside the level.
        targetX = Math.max(0, Math.min(targetX, maxCameraX));
        targetY = Math.max(0, Math.min(targetY, maxCameraY));

        // Initialize camera position.
        if (!initialized) {
            x = targetX;
            y = targetY;
            initialized = true;
            return;
        }

        // Smooth horizontal and vertical following.
        x += (targetX - x) * FOLLOW_SPEED;
        y += (targetY - y) * FOLLOW_SPEED;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZoom() {
        return ZOOM;
    }
}
