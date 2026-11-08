
package Main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

import Inputs.KeyboardInputs;
import Inputs.MouseInputs;
import entities.Player;

public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int GAME_WIDTH = 1280;
    private final int GAME_HEIGHT = 800;
    

    private Game game;

    public GamePanel(Game game) {
        this.game = game;

        setPanelSize();
        setBackground(new Color(45, 55, 80));

        KeyboardInputs keyboardInputs = new KeyboardInputs(this);
        MouseInputs mouseInputs = new MouseInputs(this);

        addKeyListener(keyboardInputs);
        addMouseListener(mouseInputs);
        addMouseMotionListener(mouseInputs);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    // PANEL SIZE
    private void setPanelSize() {
        Dimension size = new Dimension(GAME_WIDTH, GAME_HEIGHT);

        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    // DRAW GAME
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();



        game.render(g2);

        g2.dispose();
    }

    // PLAYER MOVEMENT
    public void setDirection(int direction) {
        game.getPlayer().setDirection(direction);
    }

    public void setMoving(boolean moving) {
        game.getPlayer().setMoving(moving);
    }

    public void setRunning(boolean running) {
        game.getPlayer().setRunning(running);
    }

    // PLAYER ACTIONS
    public void jump() {
        game.getPlayer().jump();
    }

    public void attack() {
        game.getPlayer().attack();
    }

    public void startBlock() {
        game.getPlayer().startBlock();
    }

    public void stopBlock() {
        game.getPlayer().stopBlock();
    }

    public void dash() {
        game.getPlayer().dash();
    }

    public void specialAttack() {
        game.getPlayer().specialAttack();
    }

    public void playHitAnimation() {
        game.getPlayer().playHitAnimation();
    }

    // TEST ANIMATIONS
    public void playGroundAnimation() {
        game.getPlayer().playGroundAnimation();
    }

    public void playDeathAnimation() {
        game.getPlayer().playDeathAnimation();
    }

    public void playVictoryAnimation() {
        game.getPlayer().playVictoryAnimation();
    }

    // GETTERS
    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return game.getPlayer();
    }
}
