package com.game.ui.screens;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;
import com.game.net.protocol.MessageTypes;
import com.game.net.protocol.Messages.CreateRoom;
import com.game.net.protocol.Messages.JoinRoom;

public class MainMenuScreen implements Screen {

    private final RingDuelGame game;
    private Stage stage;
    private Skin skin;

    public MainMenuScreen(RingDuelGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(40);
        stage.addActor(root);

        // ===== TITLE =====
        Label title = new Label("TANK BATTLE LAN", skin);
        title.setFontScale(2.2f);
        title.setAlignment(Align.center);
        root.add(title).padBottom(30).row();

        // ===== NAME INPUT =====
        TextField nameField = new TextField("", skin);
        nameField.setMessageText("Enter your name");
        root.add(nameField).width(320).padBottom(25).row();

        // ===== MAIN BUTTONS =====
        TextButton createBtn = new TextButton("CREATE ROOM", skin);
        TextButton joinBtn   = new TextButton("JOIN ROOM", skin);

        root.add(createBtn).width(260).height(50).padBottom(15).row();
        root.add(joinBtn).width(260).height(50).padBottom(30).row();

        // ===== FOOTER BUTTONS =====
        Table footer = new Table();
        TextButton settingsBtn = new TextButton("SETTINGS", skin);
        TextButton howBtn = new TextButton("HOW TO PLAY", skin);
        TextButton exitBtn = new TextButton("EXIT", skin);

        footer.add(settingsBtn).padRight(10);
        footer.add(howBtn).padRight(10);
        footer.add(exitBtn);

        root.add(footer);

        // ===== EVENTS =====
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    ensureConnected(nameField); 

                    String roomId = "ROOM_" + System.currentTimeMillis() % 1000;

                    CreateRoom cr = new CreateRoom();
                    cr.type = MessageTypes.CREATE_ROOM;
                    cr.roomId = roomId;

                    game.netClient.send(cr);
                    game.roomId = roomId;

                    game.setScreen(new RoomScreen(game));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        joinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    // 1. Kết nối server (nếu chưa)
                    ensureConnected(nameField);

                    // 2. Sang Lobby để xem danh sách phòng
                    game.setScreen(new LobbyScreen(game));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SettingsScreen(game, MainMenuScreen.this));
            }
        });

        howBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new HowToPlayScreen(game, MainMenuScreen.this));
            }
        });

        exitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
    }

    private void ensureConnected(TextField nameField) throws IOException {
        if (!game.netClient.isConnected()) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = null;

            game.playerName = name;
            game.netClient.connect("localhost", 9999, name);
        }
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
