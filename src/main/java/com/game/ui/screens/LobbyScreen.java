package com.game.ui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;
import com.game.gameplay.GameScreen;
import com.game.net.client.NetClient;
import com.game.net.protocol.MessageTypes;
import com.game.net.protocol.Messages.GameConfigMsg;
import com.game.net.protocol.Messages.JoinRoom;
import com.game.net.protocol.Messages.PeerList;
import com.game.net.protocol.Messages.RequestRoomList;
import com.game.net.protocol.Messages.RoomList;
import com.game.net.protocol.Messages.RoomUpdate;

public class LobbyScreen implements Screen {

    private RingDuelGame game;
    private Stage stage;
    private Skin skin;

    public LobbyScreen(RingDuelGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        stage.addActor(root);

        Label title = new Label("LOBBY - AVAILABLE ROOMS", skin);
        title.setFontScale(1.5f);
        root.add(title).padBottom(20).row();

        Table roomTable = new Table();
        root.add(roomTable).expand().top().row();

        // request room list
        try {
            RequestRoomList req = new RequestRoomList();
            req.type = MessageTypes.REQUEST_ROOM_LIST;
            game.netClient.send(req);
        } catch (Exception e) {
            e.printStackTrace();
        }

        game.netClient.setListener(new NetClient.Listener() {

            @Override
            public void onRoomList(RoomList rl) {
                Gdx.app.postRunnable(() -> {
                    roomTable.clearChildren();

                    if (rl.roomIds.isEmpty()) {
                        roomTable.add(new Label("No rooms available", skin));
                        return;
                    }

                    for (String roomId : rl.roomIds) {
                        TextButton roomBtn = new TextButton(roomId, skin);
                        roomBtn.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                joinRoom(roomId);
                            }
                        });
                        roomTable.add(roomBtn).width(320).height(45).pad(6).row();
                    }
                });
            }

            @Override
            public void onWelcome(int playerId) {
                game.playerId = playerId;
                System.out.println("[GAME] My playerId = " + playerId);
            }

            @Override
            public void onRoomUpdate(RoomUpdate ru) {
                // RoomScreen không cần xử lý ở đây (để trống)
            }

            @Override
            public void onPeerList(PeerList pl) {
                // Sẽ dùng ở bước UDP handshake (để trống tạm)
            }

            @Override
            public void onGameConfig(GameConfigMsg gc) {
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new GameScreen(game, gc));
                });
            }
        });
        
    }

    private void joinRoom(String roomId) {
        try {
            JoinRoom jr = new JoinRoom();
            jr.type = MessageTypes.JOIN_ROOM;
            jr.roomId = roomId;

            game.netClient.send(jr);
            game.roomId = roomId;
            game.setScreen(new RoomScreen(game));
        } catch (Exception e) {
            e.printStackTrace();
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
