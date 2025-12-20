package com.game.ui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;

public class NameInputScreen implements Screen {

    private RingDuelGame game;
    private Stage stage;
    private Skin skin;

    public NameInputScreen(RingDuelGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("TankBattle LAN", skin);
        TextField nameField = new TextField("", skin);
        nameField.setMessageText("Enter your name");

        TextButton connectBtn = new TextButton("Connect", skin);
        Label statusLabel = new Label("", skin);

        table.add(title).padBottom(20).row();
        table.add(nameField).width(300).padBottom(20).row();
        table.add(connectBtn).width(200).padBottom(20).row();
        table.add(statusLabel);

        connectBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = null;

                try {
                    game.netClient.connect("localhost", 9999, name);
                    game.playerName = name;
                    game.setScreen(new LobbyScreen(game));
                } catch (Exception e) {
                    statusLabel.setText("Failed to connect to server");
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void render(float delta) {
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
