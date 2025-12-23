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
        PlayerInfo player = null;
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
            player = new PlayerInfo(playerId, name, socket, out);

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
            // Player disconnected
            if (player != null) {
                handlePlayerDisconnect(player);
            }
        }
    }
    
    private void handlePlayerDisconnect(PlayerInfo player) {
        System.out.println(player.name + " disconnected");
        
        // Tìm room chứa player này
        Room roomToRemove = null;
        for (Room room : rooms.values()) {
            if (room.players.containsKey(player.playerId)) {
                // Nếu là host, xóa room và đẩy tất cả players ra
                if (room.hostPlayerId == player.playerId) {
                    roomToRemove = room;
                    break;
                } else {
                    // Nếu không phải host, chỉ remove player này
                    room.players.remove(player.playerId);
                    try {
                        broadcastRoomUpdate(room);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // Xóa room nếu host disconnect
        if (roomToRemove != null) {
            rooms.remove(roomToRemove.roomId);
            System.out.println("Room " + roomToRemove.roomId + " closed (host disconnected)");
            
            // Gửi ROOM_CLOSED cho tất cả players còn lại
            Messages.RoomClosed rc = new Messages.RoomClosed();
            rc.type = MessageTypes.ROOM_CLOSED;
            rc.roomId = roomToRemove.roomId;
            
            for (PlayerInfo p : roomToRemove.players.values()) {
                if (p.playerId != player.playerId) {
                    try {
                        p.out.writeObject(rc);
                        p.out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleMessage(PlayerInfo player, BaseMessage msg) throws IOException {

        switch (msg.type) {

            case CREATE_ROOM -> {
                CreateRoom cr = (CreateRoom) msg;

                if (rooms.containsKey(cr.roomId)) return;

                // update player's name if provided and reset ready flag
                if (cr.playerName != null && !cr.playerName.isEmpty()) {
                    player.name = cr.playerName;
                }
                player.ready = false;

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

                // update player's name if provided, reset ready flag, then add
                if (jr.playerName != null && !jr.playerName.isEmpty()) {
                    player.name = jr.playerName;
                }
                player.ready = false;
                room.players.put(player.playerId, player);

                System.out.println(
                    player.name + " joined room " + room.roomId
                );
                broadcastRoomUpdate(room);
            }
            case REQUEST_ROOM_LIST -> {
                RoomList rl = new RoomList();
                rl.type = MessageTypes.ROOM_LIST;
                rl.roomIds = new ArrayList<>();
                
                // Chỉ hiển thị các room chưa bắt đầu
                for (Room room : rooms.values()) {
                    if (!room.gameStarted) {
                        rl.roomIds.add(room.roomId);
                    }
                }
                // for (Room room : rooms.values()) {
                //     rl.roomIds.add(room.roomId);
                // }

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
            
            case LEAVE_ROOM -> {
                Messages.LeaveRoom lr = (Messages.LeaveRoom) msg;
                Room room = rooms.get(lr.roomId);
                if (room == null) return;
                
                // Nếu là host, xóa room và đẩy tất cả players ra
                if (room.hostPlayerId == player.playerId) {
                    rooms.remove(room.roomId);
                    System.out.println("Room " + room.roomId + " closed (host left)");
                    
                    // Gửi ROOM_CLOSED cho tất cả players còn lại
                    Messages.RoomClosed rc = new Messages.RoomClosed();
                    rc.type = MessageTypes.ROOM_CLOSED;
                    rc.roomId = room.roomId;
                    
                    for (PlayerInfo p : room.players.values()) {
                        if (p.playerId != player.playerId) {
                            p.out.writeObject(rc);
                            p.out.flush();
                        }
                    }
                } else {
                    // Nếu không phải host, chỉ remove player này
                    room.players.remove(player.playerId);
                    broadcastRoomUpdate(room);
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
        room.gameStarted = true;

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
        gc.playerNames = new HashMap<>();
        for (PlayerInfo p : room.players.values()) {
            gc.playerNames.put(p.playerId, p.name);
        }

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
