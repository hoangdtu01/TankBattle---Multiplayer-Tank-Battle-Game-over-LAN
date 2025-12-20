package com.game.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;

public class SettingsScreen implements Screen {

    private final RingDuelGame game;
    private final Screen backScreen;
    private Stage stage;
    private Skin skin;

    public SettingsScreen(RingDuelGame game, Screen backScreen) {
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

        table.add(new Label("SETTINGS", skin)).padBottom(30).row();

        CheckBox musicCheck = new CheckBox(" Music", skin);
        musicCheck.setChecked(game.settings.musicOn);

        Slider musicVol = new Slider(0f, 1f, 0.05f, false, skin);
        musicVol.setValue(game.settings.musicVolume);

        table.add(musicCheck).left().row();
        table.add(musicVol).width(300).padBottom(20).row();

        TextButton backBtn = new TextButton("BACK", skin);
        table.add(backBtn).width(200);

        musicCheck.addListener(e -> {
            game.settings.musicOn = musicCheck.isChecked();
            return false;
        });

        musicVol.addListener(e -> {
            game.settings.musicVolume = musicVol.getValue();
            return false;
        });

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

