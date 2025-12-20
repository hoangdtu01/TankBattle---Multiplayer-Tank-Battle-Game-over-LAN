
package com.game;

import com.badlogic.gdx.Game;
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

    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}

