package com.game.net.server;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerInfo {
    public int playerId;
    public String name;
    public Socket socket;
    public ObjectOutputStream out;
    public boolean ready = false;

    public PlayerInfo(int id, String name, Socket socket, ObjectOutputStream out) {
        this.playerId = id;
        this.name = name;
        this.socket = socket;
        this.out = out;
    }
}
