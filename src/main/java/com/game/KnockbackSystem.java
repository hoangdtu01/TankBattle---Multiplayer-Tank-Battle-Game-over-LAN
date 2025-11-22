package com.game;

import com.badlogic.gdx.math.Vector2;

public class KnockbackSystem {

    private final double baseForce = 220.0;
    private final double kbScale   = 18.0;
    private final double impulseScale = 10;

    public void applyRam(Player attacker, Player target) {

        if (attacker.lastRamTick == attacker.ramTickCounter) return;
        attacker.lastRamTick = attacker.ramTickCounter;

        double KB = target.knockback;
        double finalForce = baseForce + KB * kbScale;

        if (target.isShielding) finalForce *= 0.5;

        Vector2 dir = target.pos.cpy().sub(attacker.pos).nor();

        target.applyImpulse(dir.scl((float)(finalForce * impulseScale)));
        attacker.applyImpulse(dir.scl((float)(-finalForce * impulseScale * 0.25)));

        attacker.isDashing = false;
    }
}

