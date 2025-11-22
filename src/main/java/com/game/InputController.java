package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class InputController {
    private List<Player> players;

    public InputController(List<Player> players) {
        this.players = players;
    }

    public void poll() {

        Player p1 = players.get(0);
        Vector2 in1 = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) in1.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) in1.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) in1.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) in1.x += 1;
        p1.moveInput = in1;
        p1.wantShield = Gdx.input.isKeyPressed(Input.Keys.Q);
        p1.wantShoot = Gdx.input.isKeyPressed(Input.Keys.E);
        p1.wantReload = Gdx.input.isKeyJustPressed(Input.Keys.R);
        p1.wantSkill = Gdx.input.isKeyJustPressed(Input.Keys.F);

        Player p2 = players.get(1);
        Vector2 in2 = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) in2.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) in2.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) in2.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) in2.x += 1;
        p2.moveInput = in2;
        p2.wantShield = Gdx.input.isKeyPressed(Input.Keys.SLASH);
        p2.wantShoot = Gdx.input.isKeyPressed(Input.Keys.PERIOD);
        p2.wantReload = Gdx.input.isKeyJustPressed(Input.Keys.COMMA);
        p2.wantSkill = Gdx.input.isKeyJustPressed(Input.Keys.M);
    }
}

