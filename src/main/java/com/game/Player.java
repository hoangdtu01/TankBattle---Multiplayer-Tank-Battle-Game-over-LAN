package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class Player extends Entity {

    public int id;
    public float facing = 0f;
    public float radius = 18f;
    public Color color = Color.CYAN;

    public float maxSpeed = 220f;
    public float accel = 900f;
    public float friction = 6f;

    public boolean isShielding = false;
    public float maxShield = 100f;
    public float shieldDurability = maxShield;
    public float shieldBrokenTimer = 0f;
    public float shieldRegenRate = 10f; // Tốc độ hồi shield mỗi giây

    public int clipSize = 6;
    public int clip = clipSize;
    public boolean isReloading = false;
    public float reloadTime = 1f;
    public float reloadTimer = 0f;

    public float knockback = 0f;
    public float knockbackDecay = 6f;
    public float knockbackImmunityTimer = 0f; // Thời gian miễn giới hạn maxSpeed sau khi nhận knockback mạnh

    public boolean alive = true;
    public float deathTimer = 0f;

    public boolean isDashing = false;
    public float dashSpeed = 520f;
    public float dashTime = 0.3f;
    public float dashTimer = 0f;
    public int ramTickCounter = 0;
    public int lastRamTick = -1;

    public Vector2 moveInput = new Vector2();
    public boolean wantShield = false;
    public boolean wantShoot = false;
    public boolean wantReload = false;
    public boolean wantSkill = false;

    private List<Bullet> bullets;

    public Player(int id, Vector2 spawn) {
        this.id = id;
        this.pos = spawn.cpy();
        if (id == 1) color = Color.CYAN;
        else color = Color.ORANGE;
    }

    public void setBulletList(List<Bullet> bullets) {
        this.bullets = bullets;
    }

    public void update(float dt) {

        if (shieldBrokenTimer > 0f) {
            shieldBrokenTimer -= dt;
        }
        
        // Hồi shield dần dần sau khi bị vỡ
        if (shieldBrokenTimer <= 0f && shieldDurability < maxShield && !isShielding) {
            shieldDurability += shieldRegenRate * dt;
            if (shieldDurability > maxShield) {
                shieldDurability = maxShield;
            }
        }

        if (isReloading) {
            reloadTimer -= dt;
            if (reloadTimer <= 0f) {
                isReloading = false;
                clip = clipSize;
            }
        } else if (wantReload && clip < clipSize) {
            isReloading = true;
            reloadTimer = reloadTime;
        }

        if (!isReloading && wantShoot) shoot();

        // Chỉ cho phép dùng shield khi shield đã đầy
        isShielding = wantShield && shieldDurability > 0f && shieldBrokenTimer <= 0f;
        

        if (wantSkill && !isDashing && !isReloading) {
            isDashing = true;
            dashTimer = dashTime;
            ramTickCounter++;
        }

        if (isDashing) {
            dashTimer -= dt;
            Vector2 d = new Vector2((float)Math.cos(facing), (float)Math.sin(facing))
                    .scl(dashSpeed * dt);
            pos.add(d);

            if (dashTimer <= 0f) isDashing = false;

        } else {

            if (moveInput.len() > 0.001f) {
                facing = moveInput.angleRad();
                Vector2 acc = moveInput.cpy().nor().scl(accel * dt);
                vel.add(acc);
            }

            // Giảm friction khi đang bị knockback cao
            float currentFriction = friction;
            if (knockback > 90) {
                currentFriction = friction * 0.3f; // Giảm friction khi KB cao
            }

            vel.scl(1f / (1f + currentFriction * dt));

            // Cho phép vượt maxSpeed khi vừa nhận knockback mạnh hoặc KB cao
            float effectiveMaxSpeed = maxSpeed;
            if (knockbackImmunityTimer > 0f) {
                effectiveMaxSpeed = Float.MAX_VALUE; // Không giới hạn
                knockbackImmunityTimer -= dt;
            } else if (knockback >= 100f) {
                effectiveMaxSpeed = maxSpeed * 2f; // Tăng gấp 5 lần khi KB = 100
            } else if (knockback > 70f) {
                effectiveMaxSpeed = maxSpeed * 1.5f; // Tăng gấp 3 lần khi KB > 70
            } else if (knockback > 40f) {
                effectiveMaxSpeed = maxSpeed * 1f; // Tăng gấp 2 lần khi KB > 40
            }

            if (vel.len() > effectiveMaxSpeed)
                vel.nor().scl(effectiveMaxSpeed);

            pos.add(vel.cpy().scl(dt));
        }

        if (knockback > 0f)
            knockback = Math.min(100f, Math.max(0f, knockback - knockbackDecay * dt));
    }

    public void applyImpulse(Vector2 impulse) {
        vel.add(impulse);
        // Nếu impulse đủ mạnh, cho phép vượt maxSpeed trong thời gian ngắn
        // CHỈ áp dụng cho target (người nhận knockback), KHÔNG áp dụng cho attacker (phản lực)
        float impulseLen = impulse.len();
        if (impulseLen > 500f && !isDashing) { // Chỉ khi không phải đang dash (tránh phản lực từ dash)
            knockbackImmunityTimer = 0.3f; // 0.3 giây miễn giới hạn maxSpeed
        }
    }

    public boolean tryBlock(Bullet b) {
        if (!isShielding) return false;
        // Shield bây giờ là vòng tròn, block mọi hướng
        float shieldBefore = shieldDurability;
        shieldDurability -= b.impactShieldDamage;
        if (shieldDurability <= 0f) {
            shieldDurability = 0f;
            isShielding = false;
            shieldBrokenTimer = 1.5f;
            Gdx.app.log("SHIELD", String.format("P%d shield bị vỡ! Nhận damage: %.1f (%.1f -> 0.0)",
                    id, b.impactShieldDamage, shieldBefore));
        } else {
            Gdx.app.log("SHIELD", String.format("P%d shield nhận damage: %.1f (%.1f -> %.1f)",
                    id, b.impactShieldDamage, shieldBefore, shieldDurability));
        }
        return true;
    }

    private float shootCooldown = 0.12f;
    private float shootTimer = 0f;

    public void shoot() {
        shootTimer -= Gdx.graphics.getDeltaTime();
        if (isReloading) return;
        if (shootTimer > 0f) return;

        if (clip <= 0) {
            isReloading = true;
            reloadTimer = reloadTime;
            return;
        }

        shootTimer = shootCooldown;

        Vector2 dir = new Vector2((float)Math.cos(facing),(float)Math.sin(facing));
        Vector2 spawn = pos.cpy().add(dir.cpy().scl(radius+8f));
        Bullet b = new Bullet(spawn, dir, id);
        bullets.add(b);
        clip--;
    }

    public void render(ShapeRenderer sr) {
        sr.setColor(alive ? color : Color.GRAY);
        sr.circle(pos.x, pos.y, radius);

        Vector2 d = new Vector2((float)Math.cos(facing),(float)Math.sin(facing));
        sr.setColor(Color.WHITE);
        sr.rectLine(pos.x,pos.y,pos.x+d.x*(radius+16),pos.y+d.y*(radius+16),2f);

        if (isShielding) {
            sr.setColor(new Color(0f, 0.6f, 1f, 0.20f)); // opaque hơn khi người chơi chủ động bật
            sr.circle(pos.x, pos.y, radius + 24f);
        }

        sr.setColor(Color.DARK_GRAY);
        sr.rect(pos.x-20,pos.y-radius-22,40,6);
        sr.setColor(Color.CYAN);
        sr.rect(pos.x-20,pos.y-radius-22,40*(shieldDurability/maxShield),6);

        sr.setColor(Color.DARK_GRAY);
        sr.rect(pos.x-20,pos.y+radius+10,40,6);
        sr.setColor(Color.YELLOW);
        sr.rect(pos.x-20,pos.y+radius+10,40*((float)clip/clipSize),6);
    }

    public String toDebugString() {
        return String.format("P%d pos(%.1f,%.1f) KB=%.1f shield=%.1f clip=%d",
                id,pos.x,pos.y,knockback,shieldDurability,clip);
    }

    public void respawn(Arena arena) {
        if (id == 1) pos.set(arena.center.x -160, arena.center.y +60);
        else pos.set(arena.center.x +160, arena.center.y -60);

        vel.setZero();
        knockback = 0f;
        shieldDurability = maxShield;
        isShielding = false;
        alive = true;
        isDashing = false;
    }
}

