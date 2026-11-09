
package entities;
import java.awt.Color;
import java.awt.Rectangle;

public class Entity {

    protected float x;
    protected float y;
    public int width;
    public int height;
    protected Rectangle hitbox;

    public Entity(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        initHitbox();
    }

    protected void drawHitbox(java.awt.Graphics2D g) {

        g.setColor(Color.RED);
        g.drawRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
    }

    private void initHitbox() {
        hitbox = new Rectangle((int) x, (int) y, width, height);
    }

    protected  void updateHitbox() {
        hitbox.x = (int) x;
        hitbox.y = (int) y;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }



}
