package com.game;
import com.badlogic.gdx.Game;

public class RingDuelGame extends Game {
    @Override
    public void create() {
        setScreen(new GameScreen());
    }
    @Override
    public void dispose() {
        super.dispose();
    }
}

