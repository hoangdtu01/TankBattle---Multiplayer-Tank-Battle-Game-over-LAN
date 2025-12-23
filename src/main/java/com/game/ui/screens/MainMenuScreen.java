package com.game.ui.screens;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;
import com.game.net.protocol.MessageTypes;
import com.game.net.protocol.Messages.CreateRoom;
import com.game.net.protocol.Messages.JoinRoom;
import com.game.net.server.SignalingServer;
import java.net.ServerSocket;

public class MainMenuScreen implements Screen {

    private final RingDuelGame game;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private SpriteBatch batch;
    private static SignalingServer serverInstance;
    private static Thread serverThread;

    public MainMenuScreen(RingDuelGame game) {
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
        root.pad(40);
        stage.addActor(root);

        // ===== TITLE =====
        // Label title = new Label("TANK BATTLE LAN", skin);
        // title.setFontScale(2.2f);
        // title.setAlignment(Align.center);
        // root.add(title).padBottom(30).row();

        // ===== NAME INPUT =====
        TextField nameField = new TextField("", skin);
        nameField.setMessageText("Enter your name");
        nameField.getColor().a = 0.7f; // Làm mờ ô nhập
        root.add(nameField).width(260).height(50).padBottom(25).row();

        // ===== MAIN BUTTONS =====
        TextButton createBtn = new TextButton("CREATE ROOM", skin);
        TextButton joinBtn   = new TextButton("JOIN ROOM", skin);
        createBtn.getColor().a = 0.7f; // Làm mờ nút
        joinBtn.getColor().a = 0.7f; // Làm mờ nút

        root.add(createBtn).width(260).height(50).padBottom(15).row();
        root.add(joinBtn).width(260).height(50).padBottom(30).row();

        // ===== FOOTER BUTTONS =====
        Table footer = new Table();
        TextButton settingsBtn = new TextButton("SETTINGS", skin);
        TextButton howBtn = new TextButton("HOW TO PLAY", skin);
        TextButton exitBtn = new TextButton("EXIT", skin);
        settingsBtn.getColor().a = 0.7f; // Làm mờ nút
        howBtn.getColor().a = 0.7f; // Làm mờ nút
        exitBtn.getColor().a = 0.7f; // Làm mờ nút

        footer.add(settingsBtn).padRight(10);
        footer.add(howBtn).padRight(10);
        footer.add(exitBtn);

        root.add(footer);

        // ===== EVENTS =====
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    // Tự động start server nếu chưa chạy
                    startSignalingServer();
                    
                    ensureConnected(nameField); 

                    String roomId = "ROOM_" + System.currentTimeMillis() % 1000;

                    CreateRoom cr = new CreateRoom();
                    cr.type = MessageTypes.CREATE_ROOM;
                    cr.roomId = roomId;
                    // include current name so server updates its record
                    String name = nameField.getText().trim();
                    cr.playerName = name.isEmpty() ? null : name;

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

    private void startSignalingServer() {
        // Kiểm tra xem server đã chạy chưa
        if (serverInstance == null || serverThread == null || !serverThread.isAlive()) {
            try {
                // Kiểm tra port có đang được sử dụng không
                try (ServerSocket testSocket = new ServerSocket(9999)) {
                    testSocket.close();
                } catch (Exception e) {
                    // Port đã được sử dụng, server có thể đã chạy
                    System.out.println("Server may already be running on port 9999");
                    return;
                }
                
                serverInstance = new SignalingServer();
                serverThread = new Thread(() -> {
                    try {
                        serverInstance.start(9999);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                serverThread.setDaemon(true);
                serverThread.start();
                
                // Đợi một chút để server khởi động
                Thread.sleep(500);
                System.out.println("SignalingServer started automatically");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void ensureConnected(TextField nameField) throws IOException {
        // Always update the local player's name from the input field
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = null;
        game.playerName = name;

        // Connect if not already connected
        if (!game.netClient.isConnected()) {
            game.netClient.connect("localhost", 9999, name);
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
