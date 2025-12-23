package com.game.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;
import com.game.ui.screens.MainMenuScreen;
import com.game.ui.screens.SettingsScreen;
import com.game.net.protocol.Messages.LeaveRoom;
import com.game.net.protocol.Messages.RoomClosed;
import com.game.net.protocol.MessageTypes;
import com.game.core.Arena;
import com.game.core.Bullet;
import com.game.core.KnockbackSystem;
import com.game.core.Player;
import com.game.net.client.HolePuncher;
import com.game.net.client.PeerConnection;
import com.game.net.protocol.Messages.GameConfigMsg;
import com.game.net.protocol.Messages.InputMsg;
import com.game.net.protocol.Messages.SnapshotMsg;

import com.game.ui.hud.HUDRenderer;

import java.util.*;

public class GameScreen extends ScreenAdapter {

    private final int WORLD_W = 1280;
    private final int WORLD_H = 768;
    private final float TICK = 1f / 60f;

    private OrthographicCamera cam;
    private ShapeRenderer sr;
    private SpriteBatch batch;
    // Hoang
    private FitViewport viewport;
    private HUDRenderer hud;

    private Texture worldTexture;
    private Texture arenaTexture;
    private Texture player1Texture;
    private Texture player2Texture;
    private Texture bulletTexture;

    private Arena arena;
    private List<Player> players = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();

    private InputController inputController;
    private KnockbackSystem kbSystem;

    private float accumulator = 0f;

    // Hoang
    private BitmapFont font;

    // Hoang ===== MULTIPLAYER FLAGS =====
    private boolean isMultiplayer = false;
    private boolean isHost = false;
    private int hostPlayerId;
    private int localPlayerId;
    private long startTime;

    private PeerConnection peer;
    private HolePuncher hostUdp;

    // snapshot mới nhất từ host
    private SnapshotMsg lastSnapshot;

    // Hoang Client đỡ lag bằng Prediction + replay
    private int inputSeq = 0;
    private List<InputMsg> pendingInputs = new ArrayList<>();
    
    // Menu UI
    private RingDuelGame game;
    private Stage menuStage;
    private Skin menuSkin;
    private boolean menuOpen = false;
    private Table menuTable;
    private Table endGameTable;
    private Label endGameLabel;
    private boolean matchEnded = false;
    private float endGameTimer = 0f;
    private final float ENDGAME_AUTO_RETURN = 3f;


    public GameScreen() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, WORLD_W, WORLD_H);
        sr = new ShapeRenderer();
        batch = new SpriteBatch();
        // Hoang
        font = new BitmapFont();
        viewport = new FitViewport(WORLD_W, WORLD_H, cam);
        viewport.apply();

        arena = new Arena(new Vector2(WORLD_W/2f, WORLD_H/2f), 320f);

        // Player p1 = new Player(1, new Vector2(arena.center.x - 160, arena.center.y + 60));
        // Player p2 = new Player(2, new Vector2(arena.center.x + 160, arena.center.y - 60));
        // players.add(p1);
        // players.add(p2);


        inputController = new InputController();
        kbSystem = new KnockbackSystem();

        for (Player p: players) p.setBulletList(bullets);

        loadTextures();
        arena.setTexture(arenaTexture);
        // p1.setTexture(player1Texture);
        // p2.setTexture(player2Texture);
        Bullet.setSharedTexture(bulletTexture);

        Gdx.app.log("Info", "Ref sketch: /mnt/data/62654caa-0b68-407d-8595-193c0a8c8bfc.png");

        // Hoang
        hud = new HUDRenderer(
            sr,
            batch,
            font,
            WORLD_W,
            WORLD_H
        );
    }

    // Cleanup any network resources when leaving the game screen
    private void cleanupNetwork() {
        try {
            if (hostUdp != null) {
                try { hostUdp.close(); } catch (Exception e) { e.printStackTrace(); }
                hostUdp = null;
            }

            if (peer != null) {
                try { peer.close(); } catch (Exception e) { e.printStackTrace(); }
                peer = null;
            }

            isMultiplayer = false;
            isHost = false;
            lastSnapshot = null;
            pendingInputs.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Hoang contructor multiplayer
    // ===== MULTIPLAYER CONSTRUCTOR =====
    public GameScreen(RingDuelGame game, GameConfigMsg gc) {
        this(); // gọi constructor cũ để init toàn bộ gameplay
        this.game = game;
        setupMenu();

        players.clear();

        float radius = 160f;
        float angleStep = 360f / gc.playerIds.size();
        float angle = 0f;

        for (int pid : gc.playerIds) {
            Vector2 spawn = new Vector2(
                arena.center.x + MathUtils.cosDeg(angle) * radius,
                arena.center.y + MathUtils.sinDeg(angle) * radius
            );

            Player p = new Player(pid, spawn);
            p.setBulletList(bullets);
            p.allowAuthoritativeActions = (pid == gc.hostPlayerId);
            
            // Gán texture cho player dựa trên ID (luân phiên giữa player1 và player2)
            if (pid % 2 == 1) {
                p.setTexture(player1Texture);
            } else {
                p.setTexture(player2Texture);
            }
            
            // set player name from game config if available
            if (gc.playerNames != null && gc.playerNames.containsKey(pid)) {
                p.name = gc.playerNames.get(pid);
            } else if (game != null && game.playerId > 0 && pid == game.playerId && game.playerName != null && !game.playerName.isEmpty()) {
                p.name = game.playerName;
            } else {
                p.name = "P" + pid;
            }

            players.add(p);

            angle += angleStep;
        }

        // ===== LƯU CONFIG MULTIPLAYER =====
        this.isMultiplayer = true;
        this.hostPlayerId = gc.hostPlayerId;
        this.startTime = gc.startTime;
        this.localPlayerId = game.playerId;
        // xác định role
        this.isHost = (localPlayerId == hostPlayerId);

        for (Player p : players) {
            p.allowAuthoritativeActions = isHost;
        }

        try {
            if (isHost) {
                // HOST
                hostUdp = new HolePuncher(7777);
                hostUdp.listenHandshake();

                // nhận INPUT từ client
                hostUdp.listenInputs(input -> {
                    Player p = getPlayerById(input.playerId);
                    if (p != null) {
                        p.moveInput.set(input.moveX, input.moveY);
                        p.wantShoot = input.shoot;
                        p.wantSkill = input.dash;
                        p.wantShield = input.shield;

                        // lưu seq cuối đã xử lý
                        p.lastProcessedSeq = input.seq;
                    }
                });

            } else {
                // CLIENT
                peer = new PeerConnection();
                // lấy IP host từ PeerList (tạm hardcode LAN)
                peer.connectToHost("127.0.0.1", 7777, game.playerId);

                // nhận SNAPSHOT
                peer.listenSnapshot(snap -> {
                    lastSnapshot = snap;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Gdx.app.log(
            "GAME",
            "Multiplayer start | Host=" + isHost + " | startTime=" + startTime
        );
    }

    private Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.id == id) return p;
        }
        return null;
    }

    private void loadTextures() {
        worldTexture = loadTexture("asset/world.png");
        arenaTexture = loadTexture("asset/arena.png");
        player1Texture = loadTexture("asset/player1.png");
        player2Texture = loadTexture("asset/player2.png");
        bulletTexture = loadTexture("asset/bullet.png");
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }
    
    private void setupMenu() {
        menuStage = new Stage(new ScreenViewport());
        menuSkin = new Skin(Gdx.files.internal("asset/uiskin.json"));
        
        // Menu button ở góc trên bên phải
        TextButton menuBtn = new TextButton("MENU", menuSkin);
        menuBtn.setPosition(WORLD_W - 120, 20);
        menuBtn.getColor().a = 0.8f;
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleMenu();
            }
        });
        menuStage.addActor(menuBtn);
        
        // Menu popup table
        menuTable = new Table();
        menuTable.setFillParent(true);
        menuTable.setVisible(false);
        // Tạo background đơn giản từ white color với alpha
        menuTable.setBackground(menuSkin.newDrawable("white", new Color(0, 0, 0, 0.8f)));
        
        TextButton mainMenuBtn = new TextButton("BACK TO MAIN MENU", menuSkin);
        mainMenuBtn.getColor().a = 0.9f;
        mainMenuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game != null) {
                    // Nếu là host, gửi LEAVE_ROOM để đóng room
                    if (isHost) {
                        try {
                            LeaveRoom lr = new LeaveRoom();
                            lr.type = MessageTypes.LEAVE_ROOM;
                            lr.roomId = game.roomId;
                            // Gửi qua TCP connection
                            if (game.netClient != null && game.netClient.isConnected()) {
                                game.netClient.send(lr);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Cleanup network resources (sockets/threads)
                    cleanupNetwork();

                    game.setScreen(new MainMenuScreen(game));
                }
            }
        });
        
        TextButton settingsBtn = new TextButton("SETTINGS", menuSkin);
        settingsBtn.getColor().a = 0.9f;
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game != null) {
                    toggleMenu();
                    game.setScreen(new SettingsScreen(game, GameScreen.this));
                }
            }
        });
        
        TextButton closeBtn = new TextButton("CLOSE", menuSkin);
        closeBtn.getColor().a = 0.9f;
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleMenu();
            }
        });
        
        menuTable.add(mainMenuBtn).width(300).height(50).padBottom(15).row();
        menuTable.add(settingsBtn).width(300).height(50).padBottom(15).row();
        menuTable.add(closeBtn).width(300).height(50);
        
        menuStage.addActor(menuTable);

        // End-game popup (hidden by default)
        endGameTable = new Table();
        endGameTable.setFillParent(true);
        endGameTable.setVisible(false);
        endGameTable.setBackground(menuSkin.newDrawable("white", new Color(0,0,0,0.85f)));

        endGameLabel = new Label("", menuSkin);
        endGameLabel.setFontScale(1.4f);
        endGameLabel.getColor().a = 0.95f;

        endGameTable.add(endGameLabel).padBottom(20).row();
        menuStage.addActor(endGameTable);
    }
    
    private void toggleMenu() {
        menuOpen = !menuOpen;
        menuTable.setVisible(menuOpen);
        if (menuOpen) {
            Gdx.input.setInputProcessor(menuStage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void render(float delta) {

        accumulator += Math.min(delta, 0.25f);
        while (accumulator >= TICK) {
            fixedTick(TICK);
            accumulator -= TICK;
        }

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.update();
        //Hoang fix kích thước màn hình
        viewport.apply();
        sr.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        batch.begin();
        // Render background world
        if (worldTexture != null) {
            batch.draw(worldTexture, 0, 0, WORLD_W, WORLD_H);
        }
        arena.renderSprite(batch);
        for (Bullet b : bullets) b.renderSprite(batch);
        for (Player p : players) p.renderSprite(batch);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Player p : players) p.render(sr);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        // arena.renderOutline(sr);
        sr.end();

        if (Gdx.input.isKeyPressed(Input.Keys.F1)) {
            for (Player p : players)
                Gdx.app.log("DBG", p.toDebugString());
        }
        
        // Handle ESC key to toggle menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (game != null) {
                toggleMenu();
            }
        }
        
        // Hoang Hud
        hud.render(players);
        
        // Render menu UI
        if (menuStage != null) {
            menuStage.act(delta);
            menuStage.draw();
        }

    }

    private void fixedTick(float dt) {
        if (matchEnded) {
            // allow auto-return timer
            if (endGameTimer > 0f) {
                endGameTimer -= dt;
                if (endGameTimer <= 0f) {
                    // auto return now
                    if (game != null && game.netClient != null && game.netClient.isConnected() && isHost) {
                        try {
                            LeaveRoom lr = new LeaveRoom();
                            lr.type = MessageTypes.LEAVE_ROOM;
                            lr.roomId = game.roomId;
                            game.netClient.send(lr);
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    cleanupNetwork();
                    if (game != null) game.setScreen(new MainMenuScreen(game));
                }
            }
        }
        Player local = getPlayerById(localPlayerId);

        // if (isMultiplayer && !isHost && local == null) {
        //     return; // đợi snapshot đầu tiên
        // }

        // ===== 0. CLIENT: áp snapshot từ host =====
        if (isMultiplayer && !isHost && lastSnapshot != null) {

            // ===== SERVER RECONCILIATION =====
            int ack = lastSnapshot.lastProcessedSeq;

            // bỏ các input đã được host xử lý
            pendingInputs.removeIf(in -> in.seq <= ack);

            // ===== REPLAY INPUT CÒN LẠI =====
            if (local != null && !pendingInputs.isEmpty()) {
                for (InputMsg in : pendingInputs) {
                    // áp lại input
                    local.moveInput.set(in.moveX, in.moveY);
                    local.wantShoot = in.shoot;
                    local.wantSkill = in.dash;
                    local.wantShield = in.shield;

                    local.updatePrediction(dt);
                }
            }

            // ===== PLAYER STATE =====
            for (Player p : players) {
                float[] s = lastSnapshot.playerState.get(p.id);
                if (s != null) {

                    // POSITION → LERP
                    Vector2 snapPos = new Vector2(s[0], s[1]);

                    // snap cứng nếu lệch quá nhiều
                    // if (p.id == localPlayerId && p.pos.dst2(snapPos) > 36f) {
                    //     p.pos.set(snapPos);
                    //     p.vel.setZero();   
                    // } else {
                    //     p.pos.lerp(snapPos, 0.35f);
                    // }

                    if (p.id == localPlayerId) {
                        // CHỈ snap nếu lệch quá xa (teleport protection)
                        if (p.pos.dst2(snapPos) > 100f) {
                            p.pos.set(snapPos);
                            p.vel.setZero();
                        }
                    } else {
                        // REMOTE PLAYER → LERP
                        p.pos.lerp(snapPos, 0.35f);
                    }
              
                    // LOGIC STATE → SET CỨNG
                    p.facing = s[2];
                    p.shieldDurability = s[3];
                    p.knockback = s[4];
                    p.alive = s[5] == 1f;
                    p.isShielding = s[6] == 1f;
                    p.clip = (int)s[7];
                    p.isReloading = s[8] == 1f;
                    // lives (if present)
                    if (s.length > 9) {
                        p.lives = (int) s[9];
                        p.eliminated = p.lives <= 0;
                    }
                }
            }

            // ===== BULLETS =====
            bullets.clear();
            for (float[] b : lastSnapshot.bullets) {
                Vector2 pos = new Vector2(b[0], b[1]);
                Vector2 dir = new Vector2(b[2], b[3]);
                bullets.add(new Bullet(pos, dir, -1));
            }

            // If host signalled match end, show popup on clients
            if (lastSnapshot.matchEnded && !matchEnded) {
                matchEnded = true;
                String winnerMsg = (lastSnapshot.winnerId > 0) ? ("Player " + lastSnapshot.winnerId + " wins!") : "No winners";
                Gdx.app.log("GAME", "Match ended (by host): " + winnerMsg);
                if (menuStage == null) setupMenu();
                endGameLabel.setText(winnerMsg);
                endGameTable.setVisible(true);
                Gdx.input.setInputProcessor(menuStage);
                endGameTimer = ENDGAME_AUTO_RETURN;
            }
        }

        // ===== 1. READ INPUT (chỉ local player) =====
        if (local != null) {
            inputController.poll(local);
        }

        //Hoang 2. CLIENT: gửi input rồi thoát
        if (isMultiplayer && !isHost) {
            try {
                InputMsg msg = new InputMsg();
                msg.playerId = local.id;
                msg.seq = inputSeq++;
                msg.moveX = local.moveInput.x;
                msg.moveY = local.moveInput.y;
                msg.shoot = local.wantShoot;
                msg.dash  = local.wantSkill;
                msg.shield = local.wantShield;

                // lưu để replay
                pendingInputs.add(msg);

                peer.sendInput(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // 3. HOST / OFFLINE: update logic
        for (Player p : players) p.update(dt);

        // 4. Arena check: fall out => lose 1 life, if lives==0 => eliminated
        for (Player p : players) {
            if (p.alive && arena.isOutside(p.pos)) {
                p.lives -= 1;
                // reset velocity
                p.vel.setZero();
                if (p.lives <= 0) {
                    p.alive = false;
                    p.eliminated = true;
                    p.deathTimer = Float.MAX_VALUE; // won't respawn
                    Gdx.app.log("GAME", "Player " + p.id + " eliminated (no lives left)");
                } else {
                    p.alive = false;
                    p.deathTimer = 1.2f; // thời gian trước khi respawn
                    Gdx.app.log("GAME", "Player " + p.id + " fell out of arena and lost a life. Lives left=" + p.lives);
                }
            }
        }

        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.step(dt);
            if (!b.active) { it.remove(); continue; }

            for (Player p : players) {
                if (p.id == b.ownerId) continue;
                if (!p.alive) continue;
                if (b.pos.dst(p.pos) <= b.radius + p.radius) {
                    boolean blocked = p.tryBlock(b);
                    double kbGain = 8.0;
                    if (blocked) kbGain *= 0.5;
                    double kbBefore = p.knockback;
                    p.knockback += kbGain;
                    if (p.knockback > 100) p.knockback = 100;

                    Gdx.app.log("KB", String.format("P%d nhận KB: +%.1f (%.1f -> %.1f) [Blocked: %s]",
                            p.id, kbGain, kbBefore, p.knockback, blocked ? "Yes" : "No"));

                    p.applyImpulse(b.dir.scl((float)(kbGain * 0.09)));

                    b.active = false;
                    break;
                }
            }

            if (!b.active) { it.remove(); continue; }
            if (b.remainingRange <= 0f) { it.remove(); continue; }
        }

        for (Player attacker : players) {
            if (!attacker.isDashing) continue;
            for (Player target : players) {
                if (attacker == target) continue;
                if (!target.alive) continue;

                if (attacker.pos.dst(target.pos) <= attacker.radius + target.radius + 8f) {
                    kbSystem.applyRam(attacker, target);
                }
            }
        }

        for (Player p : players) {
            if (!p.alive && p.lives > 0) {
                p.deathTimer -= dt;
                if (p.deathTimer <= 0f) p.respawn(arena);
            }
        }

        // Check for end of game: only one player with lives > 0 remains
        int alivePlayers = 0;
        int lastAliveId = -1;
        for (Player p : players) {
            if (p.lives > 0) {
                alivePlayers++;
                lastAliveId = p.id;
            }
        }

        if (alivePlayers <= 1) {
            // Show end-game popup (first time only)
            if (!matchEnded) {
                matchEnded = true;
                String winnerMsg = (alivePlayers == 1) ? ("Player " + lastAliveId + " wins!") : "No winners";
                Gdx.app.log("GAME", "Match ended: " + winnerMsg);

                // Ensure menu UI exists
                if (menuStage == null) setupMenu();

                endGameLabel.setText(winnerMsg);
                endGameTable.setVisible(true);
                Gdx.input.setInputProcessor(menuStage);

                // auto return after delay
                endGameTimer = ENDGAME_AUTO_RETURN;
            }
        }

        //Hoang HOST: broadcast snapshot 
        if (isMultiplayer && isHost) {
            try {
                SnapshotMsg snap = new SnapshotMsg();
                snap.playerState = new HashMap<>();

                // ACK cho client
                snap.lastProcessedSeq = local != null ? local.lastProcessedSeq : -1;

                for (Player p : players) {
                    snap.playerState.put(
                        p.id,
                        new float[]{
                            p.pos.x,
                            p.pos.y,
                            p.facing,
                            p.shieldDurability,
                            p.knockback,
                            p.alive ? 1f : 0f,
                            p.isShielding ? 1f : 0f,
                            p.clip,
                            p.isReloading ? 1f : 0f,
                            (float)p.lives
                        }
                    );
                }

                // include match end flag/winner so clients can show popup
                snap.matchEnded = matchEnded;
                snap.winnerId = (alivePlayers == 1) ? lastAliveId : -1;

                snap.bullets = new ArrayList<>();
                for (Bullet b : bullets) {
                    snap.bullets.add(new float[]{
                        b.pos.x,
                        b.pos.y,
                        b.dir.x,
                        b.dir.y
                    });
                }

                if (hostUdp != null) {
                    hostUdp.broadcastSnapshot(snap);
                } else {
                    Gdx.app.log("GAME", "hostUdp is null — cannot broadcast snapshot");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //Hoang
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (menuStage != null) {
            menuStage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        // ensure network sockets are closed when disposing the screen
        cleanupNetwork();
        if (sr != null) sr.dispose();
        if (batch != null) batch.dispose();
        if (worldTexture != null) worldTexture.dispose();
        if (arenaTexture != null) arenaTexture.dispose();
        if (player1Texture != null) player1Texture.dispose();
        if (player2Texture != null) player2Texture.dispose();
        if (bulletTexture != null) bulletTexture.dispose();
        if (menuStage != null) menuStage.dispose();
        if (menuSkin != null) menuSkin.dispose();
    }
}
