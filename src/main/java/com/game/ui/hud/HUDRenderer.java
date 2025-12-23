package com.game.ui.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.game.core.Player;

import java.util.List;

public class HUDRenderer {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private final float barWidth = 160f;
    private final float barHeight = 12f;
    private final float padding = 20f;

    private float worldWidth;
    private float worldHeight;

    public HUDRenderer(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       float worldWidth, float worldHeight) {

        this.shapeRenderer = sr;
        this.batch = batch;
        this.font = font;

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    // ============================================================
    // PUBLIC DRAW METHOD - gọi từ GameScreen
    // ============================================================

    public void render(List<Player> players) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < players.size(); i++) {
            drawPlayerBars(players.get(i), i);
        }
        shapeRenderer.end();

        batch.begin();
        for (int i = 0; i < players.size(); i++) {
            drawPlayerText(players.get(i), i);
        }
        batch.end();
    }

    // ============================================================
    // DRAW BARS
    // ============================================================

    private void drawPlayerBars(Player p, int index) {
        float x = 0, y = 0;

        switch (index) {
            case 0: // Top-left
                x = padding;
                y = worldHeight - padding - 60;
                break;
            case 1: // Top-right
                x = worldWidth - padding - barWidth;
                y = worldHeight - padding - 60;
                break;
            case 2: // Bottom-left
                x = padding;
                y = padding + 60;
                break;
            case 3: // Bottom-right
                x = worldWidth - padding - barWidth;
                y = padding + 60;
                break;
        }

        drawShieldBar(p, x, y);
        drawAmmoBar(p, x, y - 18);
        drawKnockbackBar(p, x, y + 18);
    }

    // ============================================================
    // INDIVIDUAL BARS
    // ============================================================

    private void drawShieldBar(Player p, float x, float y) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        float ratio = p.shieldDurability / p.maxShield;
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(x, y, barWidth * ratio, barHeight);
    }

    private void drawAmmoBar(Player p, float x, float y) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        float ratio = (float) p.clip / p.clipSize;
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(x, y, barWidth * ratio, barHeight);
    }

    private void drawKnockbackBar(Player p, float x, float y) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        // Knockback color transition
        float kb = p.knockback / 100f;
        Color kbColor = new Color(
                Math.min(1f, kb),
                Math.max(0f, 1f - kb * 0.8f),
                0f,
                1f
        );
        shapeRenderer.setColor(kbColor);
        shapeRenderer.rect(x, y, barWidth * kb, barHeight);
    }

    // ============================================================
    // TEXT DISPLAY
    // ============================================================

    private void drawPlayerText(Player p, int index) {
        float x = 0, y = 0;

        switch (index) {
            case 0: x = padding; y = worldHeight - padding - 20; break;
            case 1: x = worldWidth - padding - 160; y = worldHeight - padding - 20; break;
            case 2: x = padding; y = padding + 120; break;
            case 3: x = worldWidth - padding - 160; y = padding + 120; break;
        }

        // Draw player name
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        font.draw(batch, p.name, x, y + 18);

        // Draw KB and lives
        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);
        font.draw(batch, "KB: " + (int)p.knockback + "%", x, y);

        font.setColor(Color.RED);
        font.getData().setScale(1.0f);
        font.draw(batch, "Lives: " + p.lives, x + 100, y);
    }
}
