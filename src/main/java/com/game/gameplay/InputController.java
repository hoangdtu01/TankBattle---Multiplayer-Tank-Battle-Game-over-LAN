package com.game.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.game.core.Player;

public class InputController {

    public void poll(Player p) {
        if (p == null) return;

        Vector2 input = new Vector2();

        // ===== MOVE =====
        if (Gdx.input.isKeyPressed(Input.Keys.W)) input.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) input.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) input.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) input.x += 1;

        p.moveInput.set(input);

        // ===== ACTIONS =====
        p.wantShoot  = Gdx.input.isKeyPressed(Input.Keys.J);
        p.wantShield = Gdx.input.isKeyPressed(Input.Keys.L);
        p.wantReload = Gdx.input.isKeyJustPressed(Input.Keys.R);
        p.wantSkill  = Gdx.input.isKeyJustPressed(Input.Keys.K);
    }
}
