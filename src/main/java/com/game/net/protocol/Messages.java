package com.game.net.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.game.net.protocol.Messages.BaseMessage;

import java.util.List;

public class Messages {

    public static class BaseMessage implements Serializable {
        public MessageTypes type;
    }

    // Client -> Server
    public static class Hello extends BaseMessage {
        public String playerName;
    }

    public static class JoinRoom extends BaseMessage {
        public String roomId;
        public String playerName; // optional, to update server-side name
    }

    public static class Ready extends BaseMessage {
        public String roomId;
        public boolean ready;
    }

    public static class CreateRoom extends BaseMessage {
        public String roomId;
        public String playerName; // optional, to update server-side name
    }

    public static class RequestRoomList extends BaseMessage {
    }

    public static class RoomList extends BaseMessage {
        public List<String> roomIds;
    }

    // Server -> Client
    public static class Welcome extends BaseMessage {
        public int playerId;
    }

    public static class RoomUpdate extends BaseMessage {
        public String roomId;
        public Map<Integer, String> players;   // id -> name
        public Map<Integer, Boolean> ready;    // id -> ready
    }

    public static class PeerList extends BaseMessage {
        public Map<Integer, String> peerIp;    // id -> LAN IP
        public int hostPlayerId;
    }

    public static class GameConfigMsg extends BaseMessage {
        public long mapSeed;
        public int hostPlayerId;
        public long startTime;
        public List<Integer> playerIds;
        public java.util.Map<Integer, String> playerNames; // id -> name
    }

    public static class RequestRoomState extends BaseMessage {
        public String roomId;
        public String playerName; // optional, to update server-side name
    }
    
    public static class LeaveRoom extends BaseMessage {
        public String roomId;
    }
    
    public static class RoomClosed extends BaseMessage {
        public String roomId;
    }
    
    // ===== GAMEPLAY UDP =====
    public static class InputMsg implements Serializable {
        public int playerId;
        public int seq;
        public float moveX;
        public float moveY;
        public boolean shoot;
        public boolean dash;
        public boolean shield;
    }

    public static class SnapshotMsg implements Serializable {
        public int lastProcessedSeq;
        public HashMap<Integer, float[]> playerState;  // id -> {x, y, facing, shield, knockback, alive}
        public List<float[]> bullets;   // bullet: {x, y, dirX, dirY}
        // match end flag (host sets this)
        public boolean matchEnded = false;
        public int winnerId = -1;
    }

    // ===== UDP HANDSHAKE =====
    public static class UdpHello implements Serializable {
        public int playerId;
        public int udpPort;
    }

    public static class UdpAck implements Serializable {
        public int hostPlayerId;
    }

}
