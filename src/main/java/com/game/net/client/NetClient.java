package com.game.net.client;

import java.io.*;
import java.net.Socket;

import com.game.net.protocol.MessageTypes;
import com.game.net.protocol.Messages.*;

public class NetClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Integer assignedPlayerId = null;

    public interface Listener {
        void onWelcome(int playerId);
        void onRoomUpdate(RoomUpdate ru);
        void onPeerList(PeerList pl);
        void onGameConfig(GameConfigMsg gc);
        void onRoomList(RoomList rl);
        void onRoomClosed(RoomClosed rc);
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;

        // nếu WELCOME đến trước, phát lại ngay
        if (assignedPlayerId != null) {
            listener.onWelcome(assignedPlayerId);
        }
    }

    public void connect(String serverIp, int port, String playerName) throws IOException {
        socket = new Socket(serverIp, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        Hello hello = new Hello();
        hello.type = MessageTypes.HELLO;
        hello.playerName = playerName;
        send(hello);

        new Thread(this::listen).start();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void send(BaseMessage msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    private void listen() {
        try {
            while (true) {
                BaseMessage msg = (BaseMessage) in.readObject();
                System.out.println("[TCP] Received msg type = " + msg.type);

                if (msg.type == MessageTypes.WELCOME) {
                    Welcome w = (Welcome) msg;
                    System.out.println("[TCP] Assigned playerId = " + w.playerId);
                    assignedPlayerId = w.playerId;
                    if (listener != null) {
                        listener.onWelcome(w.playerId);
                    }
                    
                    continue;
                }

                if (listener == null) continue;

                switch (msg.type) {
                    case ROOM_UPDATE -> listener.onRoomUpdate((RoomUpdate) msg);
                    case ROOM_LIST -> listener.onRoomList((RoomList) msg);
                    case PEER_LIST -> {
                        System.out.println("[TCP] Received PeerList");
                        listener.onPeerList((PeerList) msg);
                    }
                    case GAME_CONFIG -> {
                        System.out.println("[TCP] Received GameConfig");
                        listener.onGameConfig((GameConfigMsg) msg);
                    }
                    case ROOM_CLOSED -> {
                        System.out.println("[TCP] Received RoomClosed");
                        listener.onRoomClosed((RoomClosed) msg);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
