package com.game.net.server;

import java.util.*;

public class Room {
    public String roomId;
    public Map<Integer, PlayerInfo> players = new LinkedHashMap<>();
    public int hostPlayerId;
    public boolean gameStarted = false;

    public Room(String roomId, PlayerInfo host) {
        this.roomId = roomId;
        this.hostPlayerId = host.playerId;
        players.put(host.playerId, host);
    }

    public boolean allReady() {
        return players.values().stream().allMatch(p -> p.ready);
    }
}
