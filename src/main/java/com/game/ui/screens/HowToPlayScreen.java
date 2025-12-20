package com.game.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;

public class HowToPlayScreen implements Screen {

    private final RingDuelGame game;
    private final Screen backScreen;
    private Stage stage;
    private Skin skin;

    public HowToPlayScreen(RingDuelGame game, Screen backScreen) {
        this.game = game;
        this.backScreen = backScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        table.pad(40);
        stage.addActor(table);

        table.add(new Label("HOW TO PLAY", skin)).padBottom(30).row();

        table.add(new Label(
                "Move: W A S D\n" +
                "Shoot: J\n" +
                "Dash: K\n" +
                "Shield: L\n" +
                "Reload: R\n\n" +
                "Goal: Knock opponents out of the arena!",
                skin
        )).padBottom(30).row();

        TextButton backBtn = new TextButton("BACK", skin);
        table.add(backBtn).width(200);

        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(backScreen);
            }
        });
    }

    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { stage.dispose(); skin.dispose(); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}

