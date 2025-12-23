package com.game.ui.screens;

import java.util.Map;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;
import com.game.net.client.NetClient;
import com.game.net.protocol.Messages.RoomUpdate;
import com.game.net.protocol.Messages.PeerList;
import com.game.net.protocol.Messages.GameConfigMsg;
import com.game.net.protocol.MessageTypes;
import com.game.net.protocol.Messages.Ready;
import com.game.net.protocol.Messages.RequestRoomState;
import com.game.net.protocol.Messages.RoomList;
import com.game.net.protocol.Messages.LeaveRoom;
import com.game.net.protocol.Messages.RoomClosed;
import com.game.gameplay.GameScreen;

public class RoomScreen implements Screen {

    private RingDuelGame game;
    private Stage stage;
    private Skin skin;
    private boolean ready = false;
    private Texture backgroundTexture;
    private SpriteBatch batch;

    public RoomScreen(RingDuelGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("asset/mainmenu.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        stage.addActor(root);

        // ===== ROOM TITLE =====
        Label roomTitle = new Label("ROOM: " + game.roomId, skin);
        roomTitle.setFontScale(1.4f);
        roomTitle.getColor().a = 0.7f;
        root.add(roomTitle).padBottom(20).row();

        // ===== PLAYER LIST =====
        Table playerTable = new Table();
        root.add(playerTable).padBottom(20).row();

        Label status = new Label("Waiting...", skin);
        status.getColor().a = 0.7f;
        root.add(status).padBottom(10).row();

        TextButton readyBtn = new TextButton("READY", skin);
        readyBtn.getColor().a = 0.7f;
        root.add(readyBtn).width(200).height(45).padBottom(10).row();
        
        // ===== BACK BUTTON =====
        TextButton backBtn = new TextButton("BACK", skin);
        backBtn.getColor().a = 0.7f;
        root.add(backBtn).width(200).height(45);
        
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Send LEAVE_ROOM message to server
                try {
                    LeaveRoom lr = new LeaveRoom();
                    lr.type = MessageTypes.LEAVE_ROOM;
                    lr.roomId = game.roomId;
                    game.netClient.send(lr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                game.setScreen(new LobbyScreen(game));
            }
        });

        readyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ready = !ready;
                status.setText(ready ? "READY" : "Waiting...");
                readyBtn.setText(ready ? "UNREADY" : "READY");

                try {
                    Ready msg = new Ready();
                    msg.type = MessageTypes.READY;
                    msg.roomId = game.roomId;
                    msg.ready = ready;

                    game.netClient.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        game.netClient.setListener(new NetClient.Listener() {

            @Override
            public void onWelcome(int playerId) {
                game.playerId = playerId;
                System.out.println("[GAME] My playerId = " + playerId);
            }

            @Override
            public void onRoomUpdate(RoomUpdate ru) {
                System.out.println(
                    "ROOM_UPDATE:  " + ru.roomId + " : " + ru.players.values()
                );

                if (!ru.roomId.equals(game.roomId)) return;

                Gdx.app.postRunnable(() -> {
                    playerTable.clearChildren();

                    for (Map.Entry<Integer, String> e : ru.players.entrySet()) {
                        String name = e.getValue();
                        boolean isReady = ru.ready.getOrDefault(e.getKey(), false);

                        Label pLabel = new Label(
                            name + (isReady ? "  Ready" : ""),
                            skin
                        );
                        pLabel.getColor().a = 0.7f;
                        playerTable.add(pLabel).left().pad(4).row();
                    }
                });
            }


            @Override
            public void onRoomList(RoomList rl) {
                // KHÔNG DÙNG Ở ROOM SCREEN  (để trống)
            }

            @Override
            public void onPeerList(PeerList pl) {
                // Sẽ dùng ở bước UDP handshake (để trống tạm)
            }

            @Override
            public void onGameConfig(GameConfigMsg gc) {
                Gdx.app.postRunnable(() -> {

                    if (game.playerId <= 0) {
                        System.err.println(" GameConfig đến trước WELCOME đợi playerId");
                        return;
                    }
                    game.setScreen(new GameScreen(game, gc));
                });
            }
            
            @Override
            public void onRoomClosed(RoomClosed rc) {
                Gdx.app.postRunnable(() -> {
                    if (rc.roomId.equals(game.roomId)) {
                        game.setScreen(new LobbyScreen(game));
                    }
                });
            }
        });

        try {
            RequestRoomState req = new RequestRoomState();
            req.type = MessageTypes.REQUEST_ROOM_STATE;
            req.roomId = game.roomId;
            game.netClient.send(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Draw background
        if (backgroundTexture != null && batch != null) {
            batch.begin();
            float width = Gdx.graphics.getWidth();
            float height = Gdx.graphics.getHeight();
            batch.draw(backgroundTexture, 0, 0, width, height);
            batch.end();
        }
        
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { 
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (batch != null) batch.dispose();
        stage.dispose(); 
        skin.dispose(); 
    }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
