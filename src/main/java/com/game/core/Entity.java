package com.game.core;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public abstract class Entity {
    public Vector2 pos = new Vector2();
    public Vector2 vel = new Vector2();
    public float radius = 8f;
    public boolean active = true;

    public abstract void render(ShapeRenderer sr);
}

