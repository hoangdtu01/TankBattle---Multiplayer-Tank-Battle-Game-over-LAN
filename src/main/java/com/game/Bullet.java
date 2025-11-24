package com.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Bullet extends Entity {

    public Vector2 dir = new Vector2();
    public float speed = 720f;
    public float remainingRange = 600f;
    public float radius = 4f;
    public int ownerId;
    public boolean active = true;
    public float baseKnock = 18f;
    public float impactShieldDamage = 10f;

    private static Texture sharedTexture;
    private Texture texture;

    public Bullet(Vector2 spawn, Vector2 dir, int ownerId) {
        this.pos = spawn.cpy();
        this.dir = dir.cpy().nor();
        this.ownerId = ownerId;
        this.texture = sharedTexture;
    }

    public static void setSharedTexture(Texture texture) {
        sharedTexture = texture;
    }

    public void step(float dt) {
        pos.mulAdd(dir, speed * dt);
        remainingRange -= speed * dt;
        if (remainingRange <= 0) active = false;
    }

    public void renderSprite(SpriteBatch batch) {
        if (texture == null) return;
        float diameter = radius * 2f;
        float rotation = dir.angleDeg();
        batch.draw(texture,
                pos.x - radius,
                pos.y - radius,
                radius,
                radius,
                diameter,
                diameter,
                1f,
                1f,
                rotation,
                0,
                0,
                texture.getWidth(),
                texture.getHeight(),
                false,
                false);
    }

    @Override
    public void render(ShapeRenderer sr) {
        if (texture != null) return;
        sr.setColor(Color.YELLOW);
        sr.circle(pos.x, pos.y, radius);
    }
}

