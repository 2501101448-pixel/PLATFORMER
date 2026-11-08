package entities;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import levels.EnemySpawn;
import levels.EnemyType;
import levels.Level;

public class Enemy {

    public static final int DRAW_SIZE = 48;

    private static final float SIGHT_RANGE = 224f;
    private static final float ATTACK_COOLDOWN = 3.0f;

    // Enemy collision dimensions.
    private static final float ENEMY_WIDTH = 32f;
    private static final float ENEMY_HEIGHT = 32f;

    // Player collision dimensions.
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;

    // Player collision-box offset inside the sprite.
    private static final float PLAYER_OFFSET_X = 16f;
    private static final float PLAYER_OFFSET_Y = 8f;

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
    private float attackImpactTimer;
    private float cooldownTimer;
    private float defeatTimer;

    private boolean attackHitApplied;

    private float moveSpeed;

    // Level used for tile collision.
    private Level level;

    // Vertical physics for ground enemies.
    private float velocityY = 0f;

    private static final float GRAVITY = 900f;
    private static final float MAX_FALL_SPEED = 500f;

    // Compatibility with older constructor calls.
    public Enemy(EnemySpawn spawn, int tileSize, MobAssets assets) {
        this(spawn, tileSize, assets, null);
    }

    // Constructor with level collision support.
    public Enemy(
            EnemySpawn spawn,
            int tileSize,
            MobAssets assets,
            Level level
    ) {

        this.type = spawn.getType();

        this.x = spawn.getColumn() * tileSize;
        this.y = spawn.getRow() * tileSize;

        this.assets = assets;
        this.level = level;

        this.animation = assets.getPreviewAnimation(type);

        this.frameIndex = 0;
        this.frameTimer = 0;

        this.moveSpeed = getMovementSpeed();
    }

    // ==========================================
    // UPDATE
    // ==========================================

    // Compatibility with older calls.
    public void update(Player player) {
        update(player, 1f / 60f);
    }

    public void update(Player player, float deltaTime) {

        if (player == null) {
            return;
        }

        deltaTime = Math.max(0f, Math.min(deltaTime, 0.05f));

        // ======================================
        // DEFEATED ENEMY
        // ======================================

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

        // Apply gravity to ground enemies.
        // This also runs while enemies are attacking
        // or outside the player's sight range.
        updateGroundPhysics(deltaTime);

        // ======================================
        // ATTACK COOLDOWN
        // ======================================

        if (cooldownTimer > 0) {
            cooldownTimer = Math.max(0, cooldownTimer - deltaTime);
        }

        // ======================================
        // CURRENT ATTACK
        // ======================================

        if (attackTimer > 0) {

            attackTimer -= deltaTime;
            attackImpactTimer -= deltaTime;

            // Only deal damage at the attack's impact point.
            if (!attackHitApplied && attackImpactTimer <= 0) {

                attackHitApplied = true;

                if (isTouchingPlayer(player) && !player.isBlocking()) {
                    player.playHitAnimation();
                }
            }

            updateAnimation(deltaTime, false);

            if (attackTimer <= 0) {

                attackTimer = 0;
                attackImpactTimer = 0;

                playAnimation(assets.getPreviewAnimation(type));
            }

            return;
        }

        // ======================================
        // PLAYER AND ENEMY POSITIONS
        // ======================================

        float playerX = getPlayerLeft(player);
        float playerY = getPlayerTop(player);

        float playerCenterX = playerX + PLAYER_WIDTH / 2f;
        float playerCenterY = playerY + PLAYER_HEIGHT / 2f;

        float enemyCenterX = x + ENEMY_WIDTH / 2f;
        float enemyCenterY = y + ENEMY_HEIGHT / 2f;

        float differenceX = playerCenterX - enemyCenterX;
        float differenceY = playerCenterY - enemyCenterY;

        float distance = (float) Math.sqrt(
                differenceX * differenceX
                + differenceY * differenceY
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
        // FACE PLAYER
        // ======================================

        if (Math.abs(differenceX) > 1f) {
            facingLeft = differenceX < 0;
        }

        // ======================================
        // PHYSICAL CONTACT
        // ======================================

        boolean touching = isTouchingPlayer(player);

        if (touching) {

            if (cooldownTimer <= 0) {
                startAttack();
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
    // COLLISION BOXES
    // ==========================================

    private float getPlayerLeft(Player player) {
        return player.getPlayerX() + PLAYER_OFFSET_X;
    }

    private float getPlayerTop(Player player) {
        return player.getPlayerY() + PLAYER_OFFSET_Y;
    }

    private boolean isTouchingPlayer(Player player) {

        float playerLeft = getPlayerLeft(player);
        float playerTop = getPlayerTop(player);

        float playerRight = playerLeft + PLAYER_WIDTH;
        float playerBottom = playerTop + PLAYER_HEIGHT;

        float enemyRight = x + ENEMY_WIDTH;
        float enemyBottom = y + ENEMY_HEIGHT;

        boolean horizontalOverlap =
                x < playerRight
                && enemyRight > playerLeft;

        boolean verticalOverlap =
                y < playerBottom
                && enemyBottom > playerTop;

        return horizontalOverlap && verticalOverlap;
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
    // MOVEMENT
    // ==========================================

    private void moveTowardPlayer(
            float differenceX,
            float differenceY,
            float distance,
            float deltaTime
    ) {

        float speed = moveSpeed;

        // Boar accelerates when approaching.
        if (type == EnemyType.BOAR) {
            speed = distance > 128f ? 45f : 66f;
        }

        float movement = speed * deltaTime;

        if (type == EnemyType.SMALL_BEE) {

            // Normalize diagonal movement so the bee
            // doesn't move faster diagonally.
            if (distance > 0) {

                float moveX = differenceX / distance * movement;
                float moveY = differenceY / distance * movement;

                moveEnemyHorizontally(moveX);
                moveEnemyVertically(moveY);
            }

        } else {

            // Ground enemies only move horizontally.
            if (Math.abs(differenceX) > 1f) {

                float direction = Math.signum(differenceX);

                float moveX = direction * Math.min(
                        Math.abs(differenceX),
                        movement
                );

                moveEnemyHorizontally(moveX);
            }
        }
    }

    // ==========================================
    // ATTACK
    // ==========================================

    private void startAttack() {

        MobAssets.Animation attackAnimation =
                assets.getAttackAnimation(type);

        playAnimation(attackAnimation);

        int frameCount = assets.getFrameCount(
                type,
                attackAnimation
        );

        // Total attack animation duration.
        attackTimer = Math.max(
                0.1f,
                frameCount * getFrameDuration()
        );

        // Damage happens halfway through the attack.
        attackImpactTimer = attackTimer * 0.5f;

        cooldownTimer = ATTACK_COOLDOWN;

        attackHitApplied = false;

        // Damage is checked at the impact point in update().
    }

    // ==========================================
    // PLAYER ATTACK / ENEMY DEFEAT
    // ==========================================

    public void hitByPlayer(Player player) {

        if (defeated || player == null) {
            return;
        }

        float playerCenterX = player.getPlayerX() + 32f;
        float playerCenterY = player.getPlayerY() + 32f;

        float enemyCenterX = x + ENEMY_WIDTH / 2f;
        float enemyCenterY = y + ENEMY_HEIGHT / 2f;

        float differenceX = playerCenterX - enemyCenterX;
        float differenceY = playerCenterY - enemyCenterY;

        if (Math.abs(differenceX) > 80f
                || Math.abs(differenceY) > 64f) {
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
        attackImpactTimer = 0;
    }

    public boolean isDefeated() {
        return defeated && defeatTimer <= 0;
    }

    // ==========================================
    // MOVEMENT ANIMATION
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

    private void playAnimation(MobAssets.Animation nextAnimation) {

        if (nextAnimation == null || animation == nextAnimation) {
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

    private void updateAnimation(float deltaTime, boolean loop) {

        int frameCount = assets.getFrameCount(type, animation);

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
    // TILE COLLISION
    // ==========================================

    private boolean collidesAt(float testX, float testY) {

        if (level == null) {
            return false;
        }

        int tileSize = level.getTileSize();

        int leftColumn = (int) Math.floor(testX / tileSize);

        int rightColumn = (int) Math.floor(
                (testX + ENEMY_WIDTH - 0.01f) / tileSize
        );

        int topRow = (int) Math.floor(testY / tileSize);

        int bottomRow = (int) Math.floor(
                (testY + ENEMY_HEIGHT - 0.01f) / tileSize
        );

        for (int row = topRow; row <= bottomRow; row++) {

            for (int column = leftColumn;
                    column <= rightColumn;
                    column++) {

                if (level.isSolidAt(column, row)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==========================================
    // HORIZONTAL TILE COLLISION
    // ==========================================

    private void moveEnemyHorizontally(float amount) {

        if (amount == 0) {
            return;
        }

        float direction = Math.signum(amount);
        float remaining = Math.abs(amount);

        while (remaining > 0) {

            float step = Math.min(1f, remaining);
            float nextX = x + direction * step;

            if (collidesAt(nextX, y)) {
                break;
            }

            x = nextX;
            remaining -= step;
        }
    }

    // ==========================================
    // VERTICAL TILE COLLISION
    // ==========================================

    private void moveEnemyVertically(float amount) {

        if (amount == 0) {
            return;
        }

        float direction = Math.signum(amount);
        float remaining = Math.abs(amount);

        while (remaining > 0) {

            float step = Math.min(1f, remaining);
            float nextY = y + direction * step;

            if (collidesAt(x, nextY)) {

                velocityY = 0;
                return;
            }

            y = nextY;
            remaining -= step;
        }
    }

    // ==========================================
    // GROUND ENEMY PHYSICS
    // ==========================================

    private void updateGroundPhysics(float deltaTime) {

        // Flying enemies do not use gravity.
        if (type == EnemyType.SMALL_BEE) {
            return;
        }

        velocityY += GRAVITY * deltaTime;
        velocityY = Math.min(velocityY, MAX_FALL_SPEED);

        moveEnemyVertically(velocityY * deltaTime);
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