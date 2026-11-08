package entities;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import utils.LoadSave;

import static utils.Constants.Directions.*;
import static utils.Constants.PlayerConstants.*;

public class Player extends Entity {

    // SPRITE ANIMATIONS
    private BufferedImage[][] animations;

    private final int drawWidth = 64;
    private final int drawHeight = 64;

    // MOVEMENT
    private final int walkSpeed = 1;
    private final int runSpeed = 2;

    private int worldWidth = 1280;
    private int worldHeight = 800;

    private boolean moving = false;
    private boolean running = false;
    private boolean facingLeft = false;

    private int playerDir = RIGHT;

    // ANIMATION
    private int playerAction = IDLE;
    private int aniIndex = 0;
    private int aniTick = 0;

    private int aniSpeed = 15;
    private int jumpAniSpeed = 5;
    private int doubleJumpAniSpeed = 3;

    private boolean animationLoop = true;

    // PLAYER STATES
    private boolean attacking = false;
    private int attackCount;
    private boolean blocking = false;
    private boolean dead = false;
    private boolean victorious = false;

    // JUMP SYSTEM
    private int jumpsUsed = 0;
    private final int MAX_JUMPS = 2;

    private float jumpOffset = 0;
    private float velocityY = 0;

    private final float jumpStrength = -9.5f;
    private final float gravity = 0.45f;

    private boolean onGround = true;

    // DASH SYSTEM
    private int dashTimer = 0;

    private final int DASH_DURATION = 20;
    private final int DASH_SPEED = 5;

    // CONSTRUCTOR
    public Player(float x, float y) {
        super(x, y);

        loadAnimations();

        if (hasAnimation(IDLE)) {
            playerAction = IDLE;
        } else {
            playerAction = findFirstAvailableAnimation();
        }
    }

    // LOAD ANIMATIONS
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
                    + "Check your sprite path, frame size, and Constants."
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

    // CHECK ANIMATION
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

        // Use another valid frame if the current one is unavailable.
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

    // WORLD BOUNDARIES
    public void setWorldBounds(int width, int height) {

        worldWidth = Math.max(0, width);
        worldHeight = Math.max(0, height);

        keepInsideWorld();
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        keepInsideWorld();
    }

    private void keepInsideWorld() {

        int maxX = Math.max(0, worldWidth - drawWidth);
        int maxY = Math.max(0, worldHeight - drawHeight);

        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));
    }

    // MOVEMENT
    public void setDirection(int direction) {

        if (isLocked()) {
            return;
        }

        if (direction != LEFT
                && direction != RIGHT
                && direction != UP
                && direction != DOWN) {
            return;
        }

        playerDir = direction;
        moving = true;

        if (direction == LEFT) {
            facingLeft = true;
        } else if (direction == RIGHT) {
            facingLeft = false;
        }

        if (onGround && !attacking && !blocking && dashTimer <= 0) {
            selectNormalAnimation();
        }
    }

    public void setMoving(boolean moving) {

        this.moving = moving;

        if (!moving && onGround && !attacking && !blocking) {
            selectNormalAnimation();
        }
    }

    public void setRunning(boolean running) {
        this.running = running;

        if (onGround && moving && !attacking && !blocking) {
            selectNormalAnimation();
        }
    }

    private void updatePosition() {

        if (!moving || isLocked() || dashTimer > 0
                || attacking || blocking) {
            return;
        }

        int speed = running ? runSpeed : walkSpeed;

        switch (playerDir) {

            case LEFT:
                x -= speed;
                break;

            case RIGHT:
                x += speed;
                break;

            case UP:
                y -= speed;
                break;

            case DOWN:
                y += speed;
                break;

            default:
                break;
        }

        keepInsideWorld();
    }

    // JUMP AND DOUBLE JUMP
    public void jump() {

        if (isLocked() || attacking || blocking || dashTimer > 0) {
            return;
        }

        if (jumpsUsed >= MAX_JUMPS) {
            return;
        }

        jumpsUsed++;

        velocityY = jumpStrength;
        onGround = false;

        if (jumpsUsed == 1) {
            playAnimation(JUMP, false);
            System.out.println("First Jump!");

        } else {
            playAnimation(DOUBLE_JUMP, false);
            System.out.println("Double Jump!");
        }
    }

    private void updateJump() {

        if (onGround || isLocked()) {
            return;
        }

        jumpOffset += velocityY;
        velocityY += gravity;

        if (velocityY > 0
                && playerAction != JUMP
                && playerAction != DOUBLE_JUMP
                && playerAction != FALL) {
            playAnimation(FALL, true);
        }

        // Landing
        if (jumpOffset >= 0) {

            jumpOffset = 0;
            velocityY = 0;

            onGround = true;
            jumpsUsed = 0;

            playAnimation(LAND, false);
        }
    }

    // NORMAL ATTACK
    public void attack() {

        if (isLocked() || blocking || attacking || dashTimer > 0) {
            return;
        }

        attacking = true;
        moving = false;
        attackCount++;

        playAnimation(ATTACK, false);
    }

    // BLOCK
    public void startBlock() {

        if (isLocked() || attacking || dashTimer > 0) {
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

        if (!isLocked() && !attacking) {
            selectNormalAnimation();
        }
    }

    // HIT ANIMATION
    public void playHitAnimation() {

        if (isLocked() || attacking || blocking) {
            return;
        }

        moving = false;

        playAnimation(HIT, false);
    }

    // GROUND ANIMATION
    public void playGroundAnimation() {

        if (isLocked() || attacking || blocking) {
            return;
        }

        moving = false;
        attacking = false;

        playAnimation(GROUND, false);
    }

    // DASH
    public void dash() {

        if (isLocked() || attacking || blocking || dashTimer > 0) {
            return;
        }

        moving = false;
        dashTimer = DASH_DURATION;

        playAnimation(DASH, false);
    }

    private void updateDash() {

        if (dashTimer <= 0) {
            return;
        }

        int direction = facingLeft ? -1 : 1;

        x += direction * DASH_SPEED;

        keepInsideWorld();

        dashTimer--;
    }

    // SPECIAL ATTACK
    public void specialAttack() {

        if (isLocked() || attacking || blocking || dashTimer > 0) {
            return;
        }

        moving = false;
        attacking = true;
        attackCount++;

        playAnimation(SPECIAL_ATTACK, false);
    }

    // DEATH
    public void playDeathAnimation() {

        if (dead || victorious) {
            return;
        }

        dead = true;
        moving = false;
        attacking = false;
        blocking = false;
        dashTimer = 0;

        playAnimation(DEATH, false);
    }

    // VICTORY
    public void playVictoryAnimation() {

        if (dead || victorious) {
            return;
        }

        victorious = true;
        moving = false;
        attacking = false;
        blocking = false;
        dashTimer = 0;

        playAnimation(VICTORY, false);
    }

    // PLAY ANIMATION
    private void playAnimation(int action, boolean loop) {

        if (!hasAnimation(action)) {
            return;
        }

        // Do not restart an animation that is already playing.
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

    // SELECT NORMAL ANIMATION
    private void selectNormalAnimation() {

        if (isLocked() || attacking || blocking || dashTimer > 0) {
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

        } else if (moving) {

            playAnimation(running ? RUNNING : WALK, true);

        } else {

            playAnimation(IDLE, true);
        }
    }

    // UPDATE ANIMATION
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

    // FINISH ANIMATION
    private void finishAnimation() {

        int finishedAction = playerAction;

        switch (finishedAction) {

            case ATTACK:
            case SPECIAL_ATTACK:

                attacking = false;
                selectNormalAnimation();
                break;

            case HIT:
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

                // Keep the final frame.
                break;

            default:
                break;
        }
    }

    // MAIN UPDATE
    public void update() {

        if (isLocked()) {
            updateAnimation();
            return;
        }

        updatePosition();
        updateJump();
        updateDash();
        updateAnimation();

        // Automatically choose idle, walk, or run.
        if (onGround && !attacking && !blocking && dashTimer <= 0
                && playerAction != LAND
                && playerAction != HIT
                && playerAction != GROUND
                && playerAction != DASH
                && playerAction != ATTACK
                && playerAction != SPECIAL_ATTACK) {

            int wantedAction = moving
                    ? (running ? RUNNING : WALK)
                    : IDLE;

            if (playerAction != wantedAction) {
                playAnimation(wantedAction, true);
            }
        }
    }

    // RENDER
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

        int drawY = (int) (y + jumpOffset);
        int drawX = (int) x;

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

    // ANIMATION SPEED SETTINGS
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

    // GETTERS
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
        return y + jumpOffset;
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