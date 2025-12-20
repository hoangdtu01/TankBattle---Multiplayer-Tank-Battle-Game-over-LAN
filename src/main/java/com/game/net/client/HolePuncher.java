package com.game.net.client;

import com.game.net.protocol.Messages.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;

public class HolePuncher {

    private DatagramSocket socket;
    private Map<Integer, InetSocketAddress> clients = new HashMap<>();

     // ===== HOST MỞ UDP PORT CỐ ĐỊNH =====
    public HolePuncher(int port) throws Exception {
        socket = new DatagramSocket(port);
        System.out.println("[UDP] Host listening on port " + port);
    }

    // ===== LẮNG NGHE HANDSHAKE =====
    public void listenHandshake() {
        new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    Object obj = deserialize(packet.getData());
                    if (obj instanceof UdpHello hello) {
                        InetSocketAddress addr =
                                new InetSocketAddress(packet.getAddress(), hello.udpPort);
                        clients.put(hello.playerId, addr);

                        System.out.println("[UDP] HELLO from player " + hello.playerId);

                        // gửi ACK lại cho client
                        UdpAck ack = new UdpAck();
                        ack.hostPlayerId = hello.playerId;
                        send(addr, ack);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public Map<Integer, InetSocketAddress> getClients() {
        return clients;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    // ===== GỬI UDP =====
    private void send(InetSocketAddress addr, Object obj) throws Exception {
        byte[] data = serialize(obj);
        DatagramPacket packet =
                new DatagramPacket(data, data.length, addr);
        socket.send(packet);
    }

    public void registerClient(int id, String ip, int port) {
        clients.put(id, new InetSocketAddress(ip, port));
    }

    public void listenInputs(InputHandler handler) {
        new Thread(() -> {
            try {
                byte[] buf = new byte[2048];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    Object obj = deserialize(packet.getData());
                    if (obj instanceof InputMsg input) {
                        handler.onInput(input);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void broadcastSnapshot(SnapshotMsg snap) throws Exception {
        byte[] data = serialize(snap);
        for (InetSocketAddress addr : clients.values()) {
            DatagramPacket p = new DatagramPacket(
                    data, data.length, addr
            );
            socket.send(p);
        }
    }

    // ===== SERIALIZE / DESERIALIZE =====
    private byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        return bos.toByteArray();
    }

    private Object deserialize(byte[] data) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data)
        );
        return ois.readObject();
    }

    public interface InputHandler {
        void onInput(InputMsg input);
    }
}
