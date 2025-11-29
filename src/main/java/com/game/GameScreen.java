package com.game;

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
import com.badlogic.gdx.math.Vector2;

import java.util.*;

public class GameScreen extends ScreenAdapter {

    private final int WORLD_W = 1280;
    private final int WORLD_H = 768;
    private final float TICK = 1f / 60f;

    private OrthographicCamera cam;
    private ShapeRenderer sr;
    private SpriteBatch batch;

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

    public GameScreen() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, WORLD_W, WORLD_H);
        sr = new ShapeRenderer();
        batch = new SpriteBatch();
        // Hoang
        font = new BitmapFont();

        arena = new Arena(new Vector2(WORLD_W/2f, WORLD_H/2f), 320f);

        Player p1 = new Player(1, new Vector2(arena.center.x - 160, arena.center.y + 60));
        Player p2 = new Player(2, new Vector2(arena.center.x + 160, arena.center.y - 60));
        players.add(p1);
        players.add(p2);

        inputController = new InputController(players);
        kbSystem = new KnockbackSystem();

        for (Player p: players) p.setBulletList(bullets);

        loadTextures();
        arena.setTexture(arenaTexture);
        p1.setTexture(player1Texture);
        p2.setTexture(player2Texture);
        Bullet.setSharedTexture(bulletTexture);

        Gdx.app.log("Info", "Ref sketch: /mnt/data/62654caa-0b68-407d-8595-193c0a8c8bfc.png");
    }

    private void loadTextures() {
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
        sr.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        batch.begin();
        arena.renderSprite(batch);
        for (Bullet b : bullets) b.renderSprite(batch);
        for (Player p : players) p.renderSprite(batch);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Player p : players) p.render(sr);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        arena.renderOutline(sr);
        sr.end();

        //Hoang - hiển thị 3 thanh đạn, % , khiên
        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawHUD(sr);
        sr.end();

        batch.begin();
        drawHUDText();
        batch.end();



        if (Gdx.input.isKeyPressed(Input.Keys.F1)) {
            for (Player p : players)
                Gdx.app.log("DBG", p.toDebugString());
        }
    }

    private void fixedTick(float dt) {

        inputController.poll();

        for (Player p : players) p.update(dt);
        for (Player p : players) {
            if (p.alive && arena.isOutside(p.pos)) {
                p.alive = false;
                p.deathTimer = 1.2f; // thời gian trước khi respawn (tùy chỉnh)
                // reset một chút vận tốc để tránh "bay tiếp" sau respawn
                p.vel.setZero();
                Gdx.app.log("GAME", "Player " + p.id + " fell out of arena at pos=" + p.pos);
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
            if (!p.alive) {
                p.deathTimer -= dt;
                if (p.deathTimer <= 0f) p.respawn(arena);
            }
        }
    }

    private void drawHUD(ShapeRenderer sr) {

        float barWidth = 160;
        float barHeight = 12;
        float padding = 20;

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);

            float x = 0, y = 0;

            switch(i) {
                case 0: // Player 1 - top left
                    x = padding;
                    y = WORLD_H - padding - 60;
                    break;
                case 1: // Player 2 - top right
                    x = WORLD_W - padding - barWidth;
                    y = WORLD_H - padding - 60;
                    break;
                case 2: // Player 3 - bottom left
                    x = padding;
                    y = padding + 60;
                    break;
                case 3: // Player 4 - bottom right
                    x = WORLD_W - padding - barWidth;
                    y = padding + 60;
                    break;
            }

            drawPlayerHUD(sr, p, x, y, barWidth, barHeight);
        }
    }

    private void drawPlayerHUD(ShapeRenderer sr, Player p,
                            float x, float y,
                            float barWidth, float barHeight) {

        // --- SHIELD BAR ---
        sr.setColor(Color.DARK_GRAY);
        sr.rect(x, y, barWidth, barHeight);

        sr.setColor(Color.CYAN);
        float shieldRatio = p.shieldDurability / p.maxShield;
        sr.rect(x, y, barWidth * shieldRatio, barHeight);


        // --- AMMO BAR ---
        float ammoY = y - 18;
        sr.setColor(Color.DARK_GRAY);
        sr.rect(x, ammoY, barWidth, barHeight);

        sr.setColor(Color.YELLOW);
        float ammoRatio = (float)p.clip / p.clipSize;
        sr.rect(x, ammoY, barWidth * ammoRatio, barHeight);


        // --- KNOCKBACK BAR ---
        float kbY = y + 18;
        sr.setColor(Color.DARK_GRAY);
        sr.rect(x, kbY, barWidth, barHeight);

        // màu KB chuyển từ xanh → vàng → đỏ khi càng cao
        Color kbColor = new Color(
                Math.min(1f, p.knockback / 100f),
                Math.max(0f, 1f - p.knockback / 50f),
                0f,
                1f
        );

        sr.setColor(kbColor);
        float kbRatio = p.knockback / 100f;
        sr.rect(x, kbY, barWidth * kbRatio, barHeight);
    }

    private void drawHUDText() {
        font.setColor(Color.WHITE);

        float padding = 20;

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);

            float x = 0, y = 0;

            switch(i) {
                case 0: x = padding; y = WORLD_H - padding - 20; break;
                case 1: x = WORLD_W - padding - 160; y = WORLD_H - padding - 20; break;
                case 2: x = padding; y = padding + 120; break;
                case 3: x = WORLD_W - padding - 160; y = padding + 120; break;
            }

            font.draw(batch,
                    "KB: " + (int)p.knockback + "%",
                    x, y
            );
        }
    }



    @Override
    public void dispose() {
        super.dispose();
        if (sr != null) sr.dispose();
        if (batch != null) batch.dispose();
        if (arenaTexture != null) arenaTexture.dispose();
        if (player1Texture != null) player1Texture.dispose();
        if (player2Texture != null) player2Texture.dispose();
        if (bulletTexture != null) bulletTexture.dispose();
    }
}
