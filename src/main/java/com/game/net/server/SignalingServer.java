package com.game.net.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.game.net.protocol.*;
import com.game.net.protocol.Messages.*;

public class SignalingServer {

    private ServerSocket serverSocket;
    private int nextPlayerId = 1;

    // private Room room; 
    private Map<String, Room> rooms = new ConcurrentHashMap<>();


    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("LAN Signaling Server started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private void handleClient(Socket socket) {
        
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            BaseMessage msg = (BaseMessage) in.readObject();

            if (msg.type != MessageTypes.HELLO) return;

            Hello hello = (Hello) msg;
            String name = (hello.playerName == null || hello.playerName.isEmpty())
                    ? "Player " + nextPlayerId
                    : hello.playerName;

            int playerId = nextPlayerId++;
            PlayerInfo player = new PlayerInfo(playerId, name, socket, out);

            System.out.println(name + " connected (" + socket.getInetAddress() + ")");
            // Gửi lại WELCOME với playerId
            Welcome welcome = new Welcome();
            welcome.type = MessageTypes.WELCOME;
            welcome.playerId = playerId;

            out.writeObject(welcome);
            out.flush();
            // Nghe tin nhắn tiếp theo
            while (true) {
                BaseMessage m = (BaseMessage) in.readObject();
                handleMessage(player, m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(PlayerInfo player, BaseMessage msg) throws IOException {

        switch (msg.type) {

            case CREATE_ROOM -> {
                CreateRoom cr = (CreateRoom) msg;

                if (rooms.containsKey(cr.roomId)) return;

                Room room = new Room(cr.roomId, player);
                rooms.put(cr.roomId, room);

                System.out.println("Room created: " + cr.roomId);
                broadcastRoomUpdate(room);
            }

            case REQUEST_ROOM_STATE -> {
                RequestRoomState req = (RequestRoomState) msg;
                Room room = rooms.get(req.roomId);
                if (room == null) return;

                RoomUpdate ru = new RoomUpdate();
                ru.type = MessageTypes.ROOM_UPDATE;
                ru.roomId = room.roomId;
                ru.players = new HashMap<>();
                ru.ready = new HashMap<>();

                for (PlayerInfo p : room.players.values()) {
                    ru.players.put(p.playerId, p.name);
                    ru.ready.put(p.playerId, p.ready);
                }

                player.out.writeObject(ru);
                player.out.flush();
            }

            case JOIN_ROOM -> {
                JoinRoom jr = (JoinRoom) msg;
                Room room = rooms.get(jr.roomId);
                if (room == null) return;

                if (room.players.size() >= 4) return;

                room.players.put(player.playerId, player);

                System.out.println(
                    player.name + " joined room " + room.roomId
                );
                broadcastRoomUpdate(room);
            }
            case REQUEST_ROOM_LIST -> {
                RoomList rl = new RoomList();
                rl.type = MessageTypes.ROOM_LIST;
                rl.roomIds = new ArrayList<>(rooms.keySet());

                player.out.writeObject(rl);
                player.out.flush();
            }

            case READY -> {
                Ready r = (Ready) msg;
                Room room = rooms.get(r.roomId);
                if (room == null) return;

                player.ready = r.ready;
                broadcastRoomUpdate(room);

                if (room.allReady() && room.players.size() >= 2) {
                    startGame(room);
                }
            }
        }
    }

    private void broadcastRoomUpdate(Room room) throws IOException {
        RoomUpdate ru = new RoomUpdate();
        ru.type = MessageTypes.ROOM_UPDATE;
        ru.roomId = room.roomId;
        ru.players = new HashMap<>();
        ru.ready = new HashMap<>();

        for (PlayerInfo p : room.players.values()) {
            ru.players.put(p.playerId, p.name);
            ru.ready.put(p.playerId, p.ready);
        }

        // for (PlayerInfo p : room.players.values()) {
        //     p.out.writeObject(ru);
        //     p.out.flush();
        // }

        broadcast(room, ru);
    }

    private void startGame(Room room) throws IOException {
        System.out.println("Start game in room " + room.roomId);

        PeerList pl = new PeerList();
        pl.type = MessageTypes.PEER_LIST;
        pl.peerIp = new HashMap<>();
        pl.hostPlayerId = room.hostPlayerId;

        for (PlayerInfo p : room.players.values()) {
            String ip = p.socket.getInetAddress().getHostAddress();
            pl.peerIp.put(p.playerId, ip);
        }

        GameConfigMsg gc = new GameConfigMsg();
        gc.type = MessageTypes.GAME_CONFIG;
        gc.mapSeed = System.currentTimeMillis();
        gc.hostPlayerId = room.hostPlayerId;
        gc.startTime = System.currentTimeMillis() + 3000;
        gc.playerIds = new ArrayList<>(room.players.keySet());

        broadcast(room, pl);
        broadcast(room, gc);
    }

    private void broadcast(Room room, BaseMessage msg) throws IOException {
        for (PlayerInfo p : room.players.values()) {
            p.out.writeObject(msg);
            p.out.flush();
        }
    }

    public static void main(String[] args) throws Exception {
        new SignalingServer().start(9999);
    }
}
