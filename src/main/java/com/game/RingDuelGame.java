
package com.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.game.core.GameSettings;
import com.game.core.GameState;
import com.game.net.client.NetClient;
import com.game.ui.screens.*;

public class RingDuelGame extends Game {

    public GameState state = GameState.NAME_INPUT;
    public NetClient netClient = new NetClient();

    public String playerName;
    public int playerId;
    public String roomId = "ROOM1"; // tạm dùng 1 phòng cho LAN demo

    public GameSettings settings = new GameSettings();
    private Music backgroundMusic;

    @Override
    public void create() {
        // Load background music
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/background-music.wav"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(settings.musicVolume);
            if (settings.musicOn) {
                backgroundMusic.play();
            }
        } catch (Exception e) {
            Gdx.app.error("Music", "Failed to load background music", e);
        }
        
        setScreen(new MainMenuScreen(this));
    }
    
    public void updateMusicSettings() {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(settings.musicVolume);
            if (settings.musicOn) {
                if (!backgroundMusic.isPlaying()) {
                    backgroundMusic.play();
                }
            } else {
                if (backgroundMusic.isPlaying()) {
                    backgroundMusic.pause();
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        super.dispose();
    }
}

