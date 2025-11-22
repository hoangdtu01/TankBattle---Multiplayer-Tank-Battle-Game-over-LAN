package com.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Main {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Ring Duel - LibGDX");
        config.setWindowedMode(1280, 768);
        config.useVsync(true);
        new Lwjgl3Application(new RingDuelGame(), config);
    }
}