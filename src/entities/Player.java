
package entities;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import levels.Level;
import utils.LoadSave;

import static utils.Constants.Directions.*;
import static utils.Constants.PlayerConstants.*;

public class Player extends Entity {

    private BufferedImage[][] animations;

    private final int drawWidth = 64;
    private final int drawHeight = 64;

    // ==========================================
    // MOVEMENT SETTINGS
    // ==========================================

    // Maximum horizontal speeds, measured per update.
    // Maximum horizontal movement speed
    private final float walkSpeed = 1.5f;
    private final float runSpeed = 2.0f;
    private final float airSpeed = 1.4f;

    // How quickly the player reaches the target speed
    private final float groundAcceleration = 0.18f;
    private final float airAcceleration = 0.10f;

    // How quickly the player slows down
    private final float groundFriction = 0.25f;
    private final float airFriction = 0.05f;

    // Horizontal velocity.
    private float velocityX = 0f;

    private int worldWidth = 1280;

    private Level level;

    // ==========================================
    // COLLISION BOX
    // ==========================================

    private static final float COLLISION_OFFSET_X = 16f;
    private static final float COLLISION_OFFSET_Y = 8f;

    private static final float COLLISION_WIDTH = 32f;
    private static final float COLLISION_HEIGHT = 48f;

    // ==========================================
    // MOVEMENT STATE
    // ==========================================

    private boolean moving = false;
    private boolean running = false;
    private boolean facingLeft = false;

    private int playerDir = RIGHT;

    // ==========================================
    // ANIMATION
    // ==========================================

    private int playerAction = IDLE;

    private int aniIndex = 0;
    private int aniTick = 0;

    private int aniSpeed = 15;
    private int jumpAniSpeed = 5;
    private int doubleJumpAniSpeed = 3;

    private boolean animationLoop = true;

    // ==========================================
    // PLAYER STATES
    // ==========================================

    private boolean attacking = false;
    private boolean hitStunned = false;

    private int attackCount;

    private boolean blocking = false;
    private boolean dead = false;
    private boolean victorious = false;

    // ==========================================
    // JUMP SETTINGS
    // ==========================================

    private int jumpsUsed = 0;

    private final int MAX_JUMPS = 2;

    private float velocityY = 0f;

    private final float jumpStrength = -7.0f;
    private final float gravity = 0.15f;
    private final float maxFallSpeed = 3.5f;

    private boolean onGround = true;

    // Coyote time and jump buffering.
    private final int COYOTE_TIME = 10;
    private final int JUMP_BUFFER_TIME = 10;

    private int coyoteTimer = 0;
    private int jumpBufferTimer = 0;

    // ==========================================
    // DASH SETTINGS
    // ==========================================

    private int dashTimer = 0;

    private final int DASH_DURATION = 20;
    private final int DASH_SPEED = 5;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Player(float x, float y) {
        super(x, y, 64, 64);

        loadAnimations();

        if (hasAnimation(IDLE)) {
            playerAction = IDLE;
        } else {
            playerAction = findFirstAvailableAnimation();
        }
    }

    // ==========================================
    // LEVEL
    // ==========================================

    public void setLevel(Level level) {
        this.level = level;
    }

    // ==========================================
    // LOAD ANIMATIONS
    // ==========================================

    private void loadAnimations() {

        animations = LoadSave.GetAnimations();

        if (animations == null) {
            animations = new BufferedImage[15][];
            System.err.println("ERROR: Animation array is null.");
            return;
        }

        if (!hasAnyAnimation()) {
            System.err.println(
                    "ERROR: No animation frames were loaded. "
                    + "Check your sprite path and frame size."
            );
        }
    }

    private boolean hasAnyAnimation() {

        for (int action = 0; action < animations.length; action++) {
            if (hasAnimation(action)) {
                return true;
            }
        }

        return false;
    }

    private int findFirstAvailableAnimation() {

        for (int action = 0; action < animations.length; action++) {
            if (hasAnimation(action)) {
                return action;
            }
        }

        return IDLE;
    }

    // ==========================================
    // ANIMATION CHECKS
    // ==========================================

    private boolean hasAnimation(int action) {

        if (animations == null
                || action < 0
                || action >= animations.length
                || animations[action] == null) {
            return false;
        }

        for (BufferedImage frame : animations[action]) {
            if (frame != null) {
                return true;
            }
        }

        return false;
    }

    private int getValidFrameCount(int action) {

        if (!hasAnimation(action)) {
            return 0;
        }

        int count = 0;

        for (BufferedImage frame : animations[action]) {
            if (frame != null) {
                count++;
            }
        }

        return count;
    }

    private BufferedImage getCurrentFrame() {

        if (!hasAnimation(playerAction)) {
            return null;
        }

        BufferedImage[] frames = animations[playerAction];

        if (aniIndex >= 0
                && aniIndex < frames.length
                && frames[aniIndex] != null) {
            return frames[aniIndex];
        }

        for (BufferedImage frame : frames) {
            if (frame != null) {
                return frame;
            }
        }

        return null;
    }

    private boolean isLocked() {
        return dead || victorious;
    }

    // ==========================================
    // WORLD BOUNDARIES
    // ==========================================

    public void setWorldBounds(int width, int height) {
        worldWidth = Math.max(0, width);
        keepInsideWorld();
    }

    public void setPosition(float x, float y) {

        this.x = x;
        this.y = y;

        velocityX = 0;
        velocityY = 0;

        jumpsUsed = 0;
        jumpBufferTimer = 0;
        coyoteTimer = 0;

        onGround = false;

        keepInsideWorld();
    }

    private void keepInsideWorld() {

        int maxX = Math.max(0, worldWidth - drawWidth);

        if (x < 0) {
            x = 0;
            velocityX = 0;
        }

        if (x > maxX) {
            x = maxX;
            velocityX = 0;
        }
    }

    // ==========================================
    // TILE COLLISION
    // ==========================================

    private boolean collidesAt(float testX, float testY) {

        if (level == null) {
            return false;
        }

        float left = testX + COLLISION_OFFSET_X;
        float right = left + COLLISION_WIDTH - 0.01f;

        float top = testY + COLLISION_OFFSET_Y;
        float bottom = top + COLLISION_HEIGHT - 0.01f;

        int tileSize = level.getTileSize();

        int leftColumn = (int) Math.floor(left / tileSize);
        int rightColumn = (int) Math.floor(right / tileSize);

        int topRow = (int) Math.floor(top / tileSize);
        int bottomRow = (int) Math.floor(bottom / tileSize);

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
    // HORIZONTAL COLLISION
    // ==========================================

    private void moveHorizontally(float amount) {

        if (amount == 0) {
            return;
        }

        float direction = Math.signum(amount);
        float remaining = Math.abs(amount);

        while (remaining > 0) {

            float step = Math.min(1f, remaining);
            float nextX = x + direction * step;

            if (collidesAt(nextX, y)) {
                velocityX = 0;
                break;
            }

            x = nextX;
            remaining -= step;
        }

        keepInsideWorld();
    }

    // ==========================================
    // VERTICAL COLLISION
    // ==========================================

    private void moveVertically(float amount) {

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

                if (direction > 0) {

                    onGround = true;
                    jumpsUsed = 0;

                    // Reset coyote time after landing.
                    coyoteTimer = COYOTE_TIME;

                    // Do not interrupt an ongoing attack.
                    if (!attacking && !blocking && !hitStunned
                            && dashTimer <= 0) {
                        playAnimation(LAND, false);
                    }
                }

                return;
            }

            y = nextY;
            remaining -= step;
        }

        if (direction > 0) {
            onGround = false;
        }
    }

    // ==========================================
    // MOVEMENT INPUT
    // ==========================================

    public void setDirection(int direction) {

        if (isLocked()) {
            return;
        }

        if (direction != LEFT && direction != RIGHT) {
            return;
        }

        playerDir = direction;
        moving = true;

        facingLeft = direction == LEFT;

        if (onGround && !attacking && !blocking
                && dashTimer <= 0 && !hitStunned) {
            selectNormalAnimation();
        }
    }

    public void setMoving(boolean moving) {

        this.moving = moving;

        if (!moving && onGround && !attacking
                && !blocking && !hitStunned) {
            selectNormalAnimation();
        }
    }

    public void setRunning(boolean running) {

        this.running = running;

        if (onGround && moving && !attacking
                && !blocking && !hitStunned) {
            selectNormalAnimation();
        }
    }

    // ==========================================
    // ACCELERATION AND FRICTION
    // ==========================================

    private void updatePosition() {

        if (isLocked() || dashTimer > 0
                || blocking || hitStunned) {
            return;
        }

        float targetSpeed = 0f;

        // Determine the target horizontal speed.
        if (moving) {

            if (onGround) {
                targetSpeed = running ? runSpeed : walkSpeed;
            } else {
                targetSpeed = airSpeed;
            }

            if (playerDir == LEFT) {
                targetSpeed *= -1;
            }
        }

        // Choose ground or air physics.
        float acceleration = onGround
                ? groundAcceleration
                : airAcceleration;

        float friction = onGround
                ? groundFriction
                : airFriction;

        if (moving) {

            // Accelerate toward the desired speed.
            velocityX = approach(
                    velocityX,
                    targetSpeed,
                    acceleration
            );

        } else {

            // Gradually slow down when no direction is pressed.
            velocityX = approach(
                    velocityX,
                    0f,
                    friction
            );
        }

        // Prevent extremely small leftover movement.
        if (Math.abs(velocityX) < 0.01f) {
            velocityX = 0f;
        }

        // Move using tile collision.
        moveHorizontally(velocityX);
    }

    // Moves a value toward a target without overshooting.
    private float approach(float current, float target, float amount) {

        if (current < target) {
            return Math.min(current + amount, target);
        }

        if (current > target) {
            return Math.max(current - amount, target);
        }

        return target;
    }

    // ==========================================
    // JUMP INPUT
    // ==========================================

    public void jump() {

        if (isLocked() || attacking || blocking
                || dashTimer > 0 || hitStunned) {
            return;
        }

        // Store the jump request for a short time.
        jumpBufferTimer = JUMP_BUFFER_TIME;
    }

    // ==========================================
    // PERFORM JUMP
    // ==========================================

    private void performJump() {

        jumpsUsed++;

        velocityY = jumpStrength;
        onGround = false;

        // A successful jump consumes coyote time.
        coyoteTimer = 0;

        if (jumpsUsed == 1) {

            playAnimation(JUMP, false);
            System.out.println("First Jump!");

        } else {

            playAnimation(DOUBLE_JUMP, false);
            System.out.println("Double Jump!");
        }
    }

    // ==========================================
    // JUMP PHYSICS
    // ==========================================

    private void updateJump() {

        if (isLocked()) {
            return;
        }

        // Check whether the player walked off a platform.
        if (onGround && !collidesAt(x, y + 1f)) {
            onGround = false;
        }

        // Refresh coyote time while grounded.
        if (onGround) {
            coyoteTimer = COYOTE_TIME;
        } else if (coyoteTimer > 0) {
            coyoteTimer--;
        }

        // Process buffered jump input.
        if (jumpBufferTimer > 0) {

            if ((onGround || coyoteTimer > 0)
                    && jumpsUsed == 0) {

                performJump();
                jumpBufferTimer = 0;

            } else if (!onGround && jumpsUsed == 1) {

                performJump();
                jumpBufferTimer = 0;
            }

        } else if (jumpBufferTimer < 0) {
            jumpBufferTimer = 0;
        }

        // Count down the jump buffer.
        if (jumpBufferTimer > 0) {
            jumpBufferTimer--;
        }

        if (onGround) {
            return;
        }

        // Apply gravity.
        velocityY += gravity;
        velocityY = Math.min(velocityY, maxFallSpeed);

        moveVertically(velocityY);

        // Switch to falling animation.
        if (velocityY > 0 && !onGround
                && !attacking && playerAction != FALL) {
            playAnimation(FALL, true);
        }
    }

    // ==========================================
    // NORMAL ATTACK
    // ==========================================

    public void attack() {

        if (isLocked() || blocking || attacking
                || dashTimer > 0 || hitStunned) {
            return;
        }

        if (!hasAnimation(ATTACK)) {
            System.err.println("ERROR: ATTACK animation is missing.");
            return;
        }

        attacking = true;
        attackCount++;

        playAnimation(ATTACK, false);
    }

    // ==========================================
    // BLOCK
    // ==========================================

    public void startBlock() {

        if (isLocked() || attacking || dashTimer > 0
                || hitStunned) {
            return;
        }

        if (!blocking) {

            blocking = true;
            moving = false;

            playAnimation(BLOCK, true);
        }
    }

    public void stopBlock() {

        if (!blocking) {
            return;
        }

        blocking = false;

        if (!isLocked() && !attacking && !hitStunned) {
            selectNormalAnimation();
        }
    }

    // ==========================================
    // HIT ANIMATION
    // ==========================================

    public void playHitAnimation() {

        if (isLocked() || attacking || blocking || hitStunned) {
            return;
        }

        hitStunned = true;
        moving = false;

        playAnimation(HIT, false);
    }

    // ==========================================
    // GROUND ANIMATION
    // ==========================================

    public void playGroundAnimation() {

        if (isLocked() || attacking || blocking || hitStunned) {
            return;
        }

        moving = false;
        attacking = false;

        playAnimation(GROUND, false);
    }

    // ==========================================
    // DASH
    // ==========================================

    public void dash() {

        if (isLocked() || attacking || blocking
                || dashTimer > 0 || hitStunned) {
            return;
        }

        moving = false;
        velocityX = 0f;

        dashTimer = DASH_DURATION;

        playAnimation(DASH, false);
    }

    private void updateDash() {

        if (dashTimer <= 0) {
            return;
        }

        int direction = facingLeft ? -1 : 1;

        moveHorizontally(direction * DASH_SPEED);

        dashTimer--;
    }

    // ==========================================
    // SPECIAL ATTACK
    // ==========================================

    public void specialAttack() {

        if (isLocked() || attacking || blocking
                || dashTimer > 0 || hitStunned) {
            return;
        }

        if (!hasAnimation(SPECIAL_ATTACK)) {
            System.err.println(
                    "ERROR: SPECIAL_ATTACK animation is missing."
            );
            return;
        }

        attacking = true;
        attackCount++;

        playAnimation(SPECIAL_ATTACK, false);
    }

    // ==========================================
    // DEATH
    // ==========================================

    public void playDeathAnimation() {

        if (dead || victorious) {
            return;
        }

        dead = true;
        moving = false;
        velocityX = 0f;

        attacking = false;
        blocking = false;
        hitStunned = false;

        dashTimer = 0;

        playAnimation(DEATH, false);
    }

    // ==========================================
    // VICTORY
    // ==========================================

    public void playVictoryAnimation() {

        if (dead || victorious) {
            return;
        }

        victorious = true;
        moving = false;
        velocityX = 0f;

        attacking = false;
        blocking = false;
        hitStunned = false;

        dashTimer = 0;

        playAnimation(VICTORY, false);
    }

    // ==========================================
    // PLAY ANIMATION
    // ==========================================

    private void playAnimation(int action, boolean loop) {

        if (!hasAnimation(action)) {
            return;
        }

        if (playerAction == action
                && animationLoop == loop
                && aniIndex < getValidFrameCount(action) - 1) {
            return;
        }

        playerAction = action;
        aniIndex = 0;
        aniTick = 0;

        animationLoop = loop;
    }

    // ==========================================
    // SELECT NORMAL ANIMATION
    // ==========================================

    private void selectNormalAnimation() {

        if (isLocked() || attacking || blocking
                || dashTimer > 0 || hitStunned) {
            return;
        }

        if (!onGround) {

            if (velocityY < 0) {

                playAnimation(
                        jumpsUsed == 2 ? DOUBLE_JUMP : JUMP,
                        true
                );

            } else {

                playAnimation(FALL, true);
            }

        } else if (Math.abs(velocityX) > 0.1f || moving) {

            playAnimation(running ? RUNNING : WALK, true);

        } else {

            playAnimation(IDLE, true);
        }
    }

    // ==========================================
    // UPDATE ANIMATION
    // ==========================================

    private void updateAnimation() {

        if (!hasAnimation(playerAction)) {
            return;
        }

        aniTick++;

        int currentSpeed = aniSpeed;

        if (playerAction == JUMP) {
            currentSpeed = jumpAniSpeed;

        } else if (playerAction == DOUBLE_JUMP) {
            currentSpeed = doubleJumpAniSpeed;
        }

        if (aniTick < currentSpeed) {
            return;
        }

        aniTick = 0;

        int frameCount = getValidFrameCount(playerAction);

        if (frameCount <= 0) {
            return;
        }

        aniIndex++;

        if (aniIndex >= frameCount) {

            if (animationLoop) {

                aniIndex = 0;

            } else {

                aniIndex = frameCount - 1;
                finishAnimation();
            }
        }
    }

    // ==========================================
    // FINISH ANIMATION
    // ==========================================

    private void finishAnimation() {

        int finishedAction = playerAction;

        switch (finishedAction) {

            case ATTACK:
            case SPECIAL_ATTACK:

                attacking = false;
                selectNormalAnimation();
                break;

            case HIT:

                hitStunned = false;
                selectNormalAnimation();
                break;

            case GROUND:
            case LAND:
            case DASH:

                selectNormalAnimation();
                break;

            case JUMP:
            case DOUBLE_JUMP:

                if (!onGround) {

                    if (velocityY > 0) {
                        playAnimation(FALL, true);
                    }

                } else {

                    playAnimation(LAND, false);
                }

                break;

            case DEATH:
            case VICTORY:
                break;

            default:
                break;
        }
    }

    // ==========================================
    // MAIN UPDATE
    // ==========================================

    public void update() {

        if (isLocked()) {
            updateAnimation();
            return;
        }
        updateHitbox();
        updatePosition();
        updateJump();
        updateDash();
        updateAnimation();

        // Update normal ground animation when no special
        // animation is currently playing.
        if (!attacking && onGround && !blocking
                && dashTimer <= 0 && !hitStunned
                && playerAction != LAND
                && playerAction != HIT
                && playerAction != GROUND
                && playerAction != DASH) {

            int wantedAction;

            if (Math.abs(velocityX) > 0.1f) {
                wantedAction = running ? RUNNING : WALK;
            } else {
                wantedAction = IDLE;
            }

            if (playerAction != wantedAction) {
                playAnimation(wantedAction, true);
            }
        }
    }

    // ==========================================
    // RENDER
    // ==========================================

    public void render(Graphics g) {

        if (g instanceof Graphics2D) {
            draw((Graphics2D) g);
        }
    }

    public void draw(Graphics2D g) {

        if (g == null) {
            return;
        }

        BufferedImage frame = getCurrentFrame();

        if (frame == null) {
            return;
        }

        int drawX = (int) x;
        int drawY = (int) y;

        if (facingLeft) {

            g.drawImage(
                    frame,
                    drawX + drawWidth,
                    drawY,
                    -drawWidth,
                    drawHeight,
                    null
            );

        } else {

            g.drawImage(
                    frame,
                    drawX,
                    drawY,
                    drawWidth,
                    drawHeight,
                    null
            );
        }
    }

    // ==========================================
    // ANIMATION SPEED SETTINGS
    // ==========================================

    public void setAnimationSpeed(int speed) {
        if (speed > 0) {
            aniSpeed = speed;
        }
    }

    public void setJumpAnimationSpeed(int speed) {
        if (speed > 0) {
            jumpAniSpeed = speed;
        }
    }

    public void setDoubleJumpAnimationSpeed(int speed) {
        if (speed > 0) {
            doubleJumpAniSpeed = speed;
        }
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public int getPlayerAction() {
        return playerAction;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean isVictorious() {
        return victorious;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public float getPlayerX() {
        return x;
    }

    public float getPlayerY() {
        return y;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public BufferedImage[][] getAnimations() {
        return animations;
    }

    public BufferedImage getAnimationFrame(int action, int frame) {

        if (!hasAnimation(action)
                || frame < 0
                || frame >= animations[action].length) {
            return null;
        }

        return animations[action][frame];
    }
}
