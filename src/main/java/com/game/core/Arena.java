package com.game.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Arena {

    public Vector2 center;
    public float radius;
    private Texture texture;

    public Arena(Vector2 center, float radius) {
        this.center = center;
        this.radius = radius;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public void renderSprite(SpriteBatch batch) {
        if (texture == null) return;
        float scale = 1.23f; // Thay đổi giá trị này để scale asset
        float diameter = radius * 2f * scale;
        float scaledRadius = radius * scale;
        batch.draw(texture, center.x - scaledRadius, center.y - scaledRadius, diameter, diameter);
    }

    public void renderFilled(ShapeRenderer sr) {
        if (texture != null) return;
        sr.setColor(new Color(0.12f,0.12f,0.12f,1));
        sr.circle(center.x, center.y, radius);
    }

    public void renderOutline(ShapeRenderer sr) {
        sr.setColor(Color.WHITE);
        sr.circle(center.x, center.y, radius);
    }

    public boolean isOutside(Vector2 p) {
        return p.dst(center) > radius;
    }
}

