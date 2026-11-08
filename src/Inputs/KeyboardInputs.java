
package Inputs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import Main.GamePanel;

import static utils.Constants.Directions.*;

public class KeyboardInputs implements KeyListener {

    private final GamePanel gamePanel;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    private boolean downPressed;

    private boolean spacePressed;
    private boolean shiftPressed;
    private boolean blockPressed;

    public KeyboardInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    // KEY PRESSED
    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_A:
                leftPressed = true;
                updateMovement();
                break;

            case KeyEvent.VK_D:
                rightPressed = true;
                updateMovement();
                break;

            case KeyEvent.VK_W:
                upPressed = true;
                updateMovement();
                break;

            case KeyEvent.VK_S:
                downPressed = true;
                updateMovement();
                break;

            case KeyEvent.VK_SHIFT:
                shiftPressed = true;
                updateMovement();
                break;

            // JUMP / DOUBLE JUMP
            case KeyEvent.VK_SPACE:
                if (!spacePressed) {
                    spacePressed = true;
                    gamePanel.jump();
                }
                break;

            // NORMAL ATTACK
            case KeyEvent.VK_J:
                gamePanel.attack();
                break;

            // SPECIAL ATTACK
            case KeyEvent.VK_K:
                gamePanel.specialAttack();
                break;

            // DASH
            case KeyEvent.VK_L:
                gamePanel.dash();
                break;

            // BLOCK
            case KeyEvent.VK_B:
                if (!blockPressed) {
                    blockPressed = true;
                    gamePanel.startBlock();
                }
                break;

            // TEST HIT
            case KeyEvent.VK_H:
                gamePanel.playHitAnimation();
                break;

            // TEST GROUND
            case KeyEvent.VK_G:
                gamePanel.playGroundAnimation();
                break;

            // TEST DEATH
            case KeyEvent.VK_X:
                gamePanel.playDeathAnimation();
                break;

            // TEST VICTORY
            case KeyEvent.VK_V:
                gamePanel.playVictoryAnimation();
                break;
        }
    }

    // UPDATE MOVEMENT
    private void updateMovement() {

        gamePanel.setRunning(shiftPressed);

        if (leftPressed) {
            gamePanel.setDirection(LEFT);

        } else if (rightPressed) {
            gamePanel.setDirection(RIGHT);

        } else if (upPressed) {
            gamePanel.setDirection(UP);

        } else if (downPressed) {
            gamePanel.setDirection(DOWN);

        } else {
            gamePanel.setMoving(false);
        }
    }

    // KEY RELEASED
    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_A:
                leftPressed = false;
                updateMovement();
                break;

            case KeyEvent.VK_D:
                rightPressed = false;
                updateMovement();
                break;

            case KeyEvent.VK_W:
                upPressed = false;
                updateMovement();
                break;

            case KeyEvent.VK_S:
                downPressed = false;
                updateMovement();
                break;

            case KeyEvent.VK_SHIFT:
                shiftPressed = false;
                updateMovement();
                break;

            case KeyEvent.VK_SPACE:
                spacePressed = false;
                break;

            case KeyEvent.VK_B:
                blockPressed = false;
                gamePanel.stopBlock();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}
