
package entities;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import levels.EnemySpawn;
import levels.EnemyType;

public class Enemy {

    public static final int DRAW_SIZE = 48;

    private static final float SIGHT_RANGE = 224f;
    private static final float ATTACK_COOLDOWN = 3.0f;

    private final EnemyType type;
    private final MobAssets assets;

    private float x;
    private float y;

    private boolean facingLeft;
    private boolean defeated;

    private MobAssets.Animation animation;

    private int frameIndex;
    private float frameTimer;

    private float attackTimer;
    private float cooldownTimer;
    private float defeatTimer;

    private float moveSpeed;

    public Enemy(EnemySpawn spawn, int tileSize, MobAssets assets) {

        this.type = spawn.getType();

        this.x = spawn.getColumn() * tileSize;
        this.y = spawn.getRow() * tileSize;

        this.assets = assets;

        this.animation = assets.getPreviewAnimation(type);

        this.frameIndex = 0;
        this.frameTimer = 0;

        this.moveSpeed = getMovementSpeed();
    }

    // ==========================================
    // UPDATE
    // ==========================================

    // Compatible with your existing GamePanel
    public void update(Player player) {
        update(player, 1f / 60f);
    }

    // Delta-time update for smoother movement
    public void update(Player player, float deltaTime) {

        if (player == null) {
            return;
        }

        // Prevent large jumps after lag or pauses
        deltaTime = Math.max(0f, Math.min(deltaTime, 0.05f));

        if (defeated) {

            if (defeatTimer > 0) {

                defeatTimer -= deltaTime;

                updateAnimation(deltaTime, false);

                if (defeatTimer <= 0) {
                    defeatTimer = 0;
                }
            }

            return;
        }

        if (cooldownTimer > 0) {
            cooldownTimer -= deltaTime;
        }

        // Attack animation
        if (attackTimer > 0) {

            attackTimer -= deltaTime;

            updateAnimation(deltaTime, false);

            if (attackTimer <= 0) {

                attackTimer = 0;

                playAnimation(assets.getPreviewAnimation(type));
            }

            return;
        }

        // Player center
        float playerCenterX = player.getPlayerX() + 32f;
        float playerCenterY = player.getPlayerY() + 32f;

        // Enemy center
        float enemyCenterX = x + DRAW_SIZE / 2f;
        float enemyCenterY = y + DRAW_SIZE / 2f;

        float differenceX = playerCenterX - enemyCenterX;
        float differenceY = playerCenterY - enemyCenterY;

        float distance = (float) Math.sqrt(
                differenceX * differenceX +
                differenceY * differenceY
        );

        // ======================================
        // PLAYER OUT OF SIGHT
        // ======================================

        if (distance > SIGHT_RANGE) {

            playAnimation(assets.getPreviewAnimation(type));

            updateAnimation(deltaTime, true);

            return;
        }

        // ======================================
        // ENEMY DIRECTION
        // ======================================

        facingLeft = differenceX < 0;

        // ======================================
        // ATTACK RANGE
        // ======================================

        float attackRange =
                type == EnemyType.SMALL_BEE ? 96f : 48f;

        boolean canAttack = distance <= attackRange;

        if (type != EnemyType.SMALL_BEE) {

            canAttack = canAttack &&
                    Math.abs(differenceY) <= 64f;
        }

        if (canAttack) {

            if (cooldownTimer <= 0) {

                startAttack(player);

            } else {

                playAnimation(assets.getPreviewAnimation(type));

                updateAnimation(deltaTime, true);
            }

            return;
        }

        // ======================================
        // CHASE PLAYER
        // ======================================

        moveTowardPlayer(
                differenceX,
                differenceY,
                distance,
                deltaTime
        );

        playAnimation(getMoveAnimation(distance));

        updateAnimation(deltaTime, true);
    }

    // ==========================================
    // MOVEMENT SPEED
    // ==========================================

    private float getMovementSpeed() {

        switch (type) {

            case BOAR:
                return 45f;

            case SNAIL:
                return 21f;

            case SMALL_BEE:
                return 39f;

            default:
                return 30f;
        }
    }

    // ==========================================
    // SMOOTH MOVEMENT
    // ==========================================

    private void moveTowardPlayer(
            float differenceX,
            float differenceY,
            float distance,
            float deltaTime
    ) {

        float speed = moveSpeed;

        // Boar runs faster when close to the player
        if (type == EnemyType.BOAR) {

            speed = distance > 128f ? 45f : 66f;
        }

        // Horizontal movement
        if (Math.abs(differenceX) > 3f) {

            x += Math.signum(differenceX) *
                    speed * deltaTime;
        }

        // Bee can fly vertically
        if (type == EnemyType.SMALL_BEE) {

            if (Math.abs(differenceY) > 3f) {

                y += Math.signum(differenceY) *
                        speed * deltaTime;
            }
        }
    }

    // ==========================================
    // ATTACK
    // ==========================================

    private void startAttack(Player player) {

        MobAssets.Animation attackAnimation =
                assets.getAttackAnimation(type);

        playAnimation(attackAnimation);

        int frameCount = assets.getFrameCount(
                type,
                attackAnimation
        );

        attackTimer = Math.max(
                0.1f,
                frameCount * getFrameDuration()
        );

        cooldownTimer = ATTACK_COOLDOWN;

        // Keeps compatibility with your existing Player class
        player.playHitAnimation();
    }

    // ==========================================
    // PLAYER ATTACK / ENEMY DEFEAT
    // ==========================================

    public void hitByPlayer(Player player) {

        if (defeated || player == null) {
            return;
        }

        float differenceX =
                player.getPlayerX() + 32f -
                (x + DRAW_SIZE / 2f);

        float differenceY =
                player.getPlayerY() + 32f -
                (y + DRAW_SIZE / 2f);

        if (Math.abs(differenceX) > 80f ||
                Math.abs(differenceY) > 64f) {

            return;
        }

        defeated = true;

        MobAssets.Animation deathAnimation;

        if (assets.hasAnimation(type, MobAssets.Animation.DEAD)) {

            deathAnimation = MobAssets.Animation.DEAD;

        } else {

            deathAnimation = MobAssets.Animation.HIT;
        }

        playAnimation(deathAnimation);

        int frameCount = assets.getFrameCount(
                type,
                deathAnimation
        );

        defeatTimer = Math.max(
                0.1f,
                frameCount * getFrameDuration()
        );

        attackTimer = 0;
    }

    public boolean isDefeated() {

        return defeated && defeatTimer <= 0;
    }

    // ==========================================
    // SELECT MOVEMENT ANIMATION
    // ==========================================

    private MobAssets.Animation getMoveAnimation(float distance) {

        switch (type) {

            case BOAR:

                return distance > 128f
                        ? MobAssets.Animation.WALK
                        : MobAssets.Animation.RUN;

            case SNAIL:

                return MobAssets.Animation.WALK;

            case SMALL_BEE:

                return MobAssets.Animation.FLY;

            default:

                return MobAssets.Animation.IDLE;
        }
    }

    // ==========================================
    // ANIMATION TRANSITION
    // ==========================================

    private void playAnimation(
            MobAssets.Animation nextAnimation
    ) {

        if (nextAnimation == null ||
                animation == nextAnimation) {

            return;
        }

        if (!assets.hasAnimation(type, nextAnimation)) {
            return;
        }

        animation = nextAnimation;

        frameIndex = 0;

        frameTimer = 0f;
    }

    // ==========================================
    // ANIMATION UPDATE
    // ==========================================

    private void updateAnimation(
            float deltaTime,
            boolean loop
    ) {

        int frameCount = assets.getFrameCount(
                type,
                animation
        );

        if (frameCount <= 1) {
            return;
        }

        frameTimer += deltaTime;

        float frameDuration = getFrameDuration();

        while (frameTimer >= frameDuration) {

            frameTimer -= frameDuration;

            if (frameIndex < frameCount - 1) {

                frameIndex++;

            } else if (loop) {

                frameIndex = 0;

            } else {

                frameIndex = frameCount - 1;

                frameTimer = 0f;

                break;
            }
        }
    }

    // ==========================================
    // FRAME DURATION
    // ==========================================

    private float getFrameDuration() {
        return 0.070f;
    }

    // ==========================================
    // DRAW ENEMY
    // ==========================================

    public void draw(Graphics2D graphics) {

        BufferedImage frame = assets.getFrame(
                type,
                animation,
                frameIndex
        );

        if (frame == null) {
            return;
        }

        int drawX = Math.round(x) - 8;
        int drawY = Math.round(y) - 16;

        if (facingLeft) {

            graphics.drawImage(
                    frame,
                    drawX + DRAW_SIZE,
                    drawY,
                    -DRAW_SIZE,
                    DRAW_SIZE,
                    null
            );

        } else {

            graphics.drawImage(
                    frame,
                    drawX,
                    drawY,
                    DRAW_SIZE,
                    DRAW_SIZE,
                    null
            );
        }
    }

    // ==========================================
    // PREVIEW ANIMATION
    // ==========================================

    public void drawPreview(Graphics2D graphics) {

        MobAssets.Animation preview =
                assets.getPreviewAnimation(type);

        int frameCount = assets.getFrameCount(type, preview);

        if (frameCount <= 0) {
            return;
        }

        long frameDuration = (long) (
                getFrameDuration() * 1_000_000_000L
        );

        int frame = (int) (
                System.nanoTime() / frameDuration % frameCount
        );

        BufferedImage image = assets.getFrame(
                type,
                preview,
                frame
        );

        if (image != null) {

            graphics.drawImage(
                    image,
                    Math.round(x) - 8,
                    Math.round(y) - 16,
                    DRAW_SIZE,
                    DRAW_SIZE,
                    null
            );
        }
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isFacingLeft() {
        return facingLeft;
    }

    public boolean isDefeatedState() {
        return defeated;
    }
}
