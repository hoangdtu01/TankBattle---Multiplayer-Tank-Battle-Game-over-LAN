package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class KnockbackSystem {

    private final double baseForce = 50.0;
    private final double kbScale   = 2.0;
    private final double impulseScale = 7;

    public void applyRam(Player attacker, Player target) {

        if (attacker.lastRamTick == attacker.ramTickCounter) return;
        attacker.lastRamTick = attacker.ramTickCounter;

        double KB = target.knockback;
        double finalForce = baseForce + KB * kbScale;
        boolean wasShielding = target.isShielding;

        // Tăng lực đáng kể khi KB cao
        if (KB >= 100f) {
            finalForce *= 1.15; // Tăng gấp 3 lần khi KB = 100 (đảm bảo văng ra map)
        } else if (KB >= 80f) {
            finalForce *= 1.15; // Tăng gấp 2 lần khi KB >= 80
        } else if (KB >= 60f) {
            finalForce *= 1.11; // Tăng 50% khi KB >= 60
        } else if (KB >= 40f) {
            finalForce *= 1.07; // Tăng 50% khi KB >= 60
        } else if (KB >= 20f) {
            finalForce *= 1.04; // Tăng 50% khi KB >= 60
        } else if (KB >= 10f) {
            finalForce *= 1; // Tăng 50% khi KB >= 60
        } 

        // Chỉ giảm lực nếu shield >= 70 và đang bật shield
        // Nếu shield < 70, nhận nguyên damage như không bật shield
        boolean shieldEffective = target.isShielding && target.shieldDurability >= 70f;
        if (shieldEffective) {
            finalForce *= 0.3;
        }

        // Trừ shield của target khi bị dash vào - CHỈ KHI ĐANG BẬT SHIELD
        float dashShieldDamage = 70f;
        if (target.isShielding && target.shieldDurability > 0f) {
            float shieldBefore = target.shieldDurability;
            target.shieldDurability -= dashShieldDamage;
            if (target.shieldDurability <= 0f) {
                target.shieldDurability = 0f;
                target.isShielding = false;
                target.shieldBrokenTimer = 1.5f;
                Gdx.app.log("DASH", String.format("P%d shield bị vỡ do dash! Nhận damage: %.1f (%.1f -> 0.0)",
                        target.id, dashShieldDamage, shieldBefore));
            } else {
                Gdx.app.log("DASH", String.format("P%d shield nhận damage từ dash: %.1f (%.1f -> %.1f)",
                        target.id, dashShieldDamage, shieldBefore, target.shieldDurability));
            }
        }

        Vector2 dir = target.pos.cpy().sub(attacker.pos).nor();

        String shieldStatus = wasShielding ? (target.shieldDurability >= 70f ? "Yes (x0.3)" : "Yes (<70, no reduction)") : "No";
        Gdx.app.log("DASH", String.format("P%d dash vào P%d | KB target: %.1f | Force: %.1f (base: %.1f + KB*%.1f) [Shield: %s] | Final force: %.1f",
                attacker.id, target.id, KB, finalForce, baseForce, KB * kbScale, shieldStatus, finalForce));

        float impulseMagnitude = (float)(finalForce * impulseScale);
        float velBefore = target.vel.len();
        target.applyImpulse(dir.scl(impulseMagnitude));
        float velAfter = target.vel.len();
        
        // Dừng dash và giảm vận tốc của attacker để tránh bị văng
        attacker.isDashing = false;
        attacker.dashTimer = 0f;
        
        // Phản lực cho attacker - giảm đáng kể và giới hạn tối đa
        float attackerImpulse = -impulseMagnitude * 0.0005f; // Giảm từ 25% xuống 5%
        float maxAttackerImpulse = 0.5f; // Giới hạn tối đa phản lực (giảm từ 800)
        if (Math.abs(attackerImpulse) > maxAttackerImpulse) {
            attackerImpulse = attackerImpulse > 0 ? maxAttackerImpulse : -maxAttackerImpulse;
        }
        
        // Giảm vận tốc hiện tại của attacker trước khi áp dụng phản lực
        attacker.vel.scl(0.3f); // Giảm 70% vận tốc hiện tại
        attacker.applyImpulse(dir.scl(attackerImpulse));

        Gdx.app.log("DASH", String.format("P%d velocity: %.1f -> %.1f (impulse: %.1f) | Attacker recoil: %.1f (vel reduced to %.1f)",
                target.id, velBefore, velAfter, impulseMagnitude, attackerImpulse, attacker.vel.len()));
    }
}

