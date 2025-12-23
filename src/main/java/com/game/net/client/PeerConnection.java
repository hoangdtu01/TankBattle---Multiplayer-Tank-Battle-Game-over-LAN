package com.game.net.client;

import com.game.net.protocol.Messages.*;

import java.io.*;
import java.net.*;

public class PeerConnection {

    private DatagramSocket socket;
    private InetSocketAddress hostAddr;

    // ===== KHỞI TẠO SOCKET UDP  =====
    public PeerConnection(int localPort) throws Exception {
        socket = new DatagramSocket(localPort);
    }

    
    public PeerConnection() throws Exception {
        socket = new DatagramSocket(); // tự chọn port LAN
        // Don't set timeout globally - only use it for handshake
    }

    // ===== HANDSHAKE VỚI HOST =====
    public void connectToHost(String hostIp, int hostPort, int playerId) throws Exception {
        hostAddr = new InetSocketAddress(hostIp, hostPort);
        
        // Set timeout for handshake only
        socket.setSoTimeout(5000);

        UdpHello hello = new UdpHello();
        hello.playerId = playerId;
        hello.udpPort = socket.getLocalPort();

        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        boolean connected = false;

        for (int i = 0; i < 5; i++) {
            send(hello);
            try {
                socket.receive(packet);

                Object obj = deserialize(packet.getData());
                if (obj instanceof UdpAck) {
                    System.out.println("[UDP] Connected to host");
                    connected = true;
                    break;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[UDP] retry handshake... (" + (i + 1) + "/5)");
            }
        }

        if (!connected) {
            throw new RuntimeException("UDP handshake failed");
        }
        
        // Remove timeout after handshake so listening thread can block indefinitely
        socket.setSoTimeout(0);
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    // ===== HÀM GỬI UDP =====
    private void send(Object obj) throws Exception {
        byte[] data = serialize(obj);
        DatagramPacket packet =
                new DatagramPacket(data, data.length, hostAddr);
        socket.send(packet);
    }

    public void sendInput(InputMsg msg) throws Exception {
        byte[] data = serialize(msg);
        DatagramPacket packet = new DatagramPacket(
                data, data.length, hostAddr
        );
        socket.send(packet);
    }

    public void listenSnapshot(SnapshotHandler handler) {
        new Thread(() -> {
            try {
                byte[] buf = new byte[2048];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                        Object obj = deserialize(packet.getData());
                        if (obj instanceof SnapshotMsg snap) {
                            handler.onSnapshot(snap);
                        }
                    } catch (SocketException se) {
                        if (socket.isClosed()) break;
                        throw se;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

    // Close socket and cleanup
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            hostAddr = null;
        }
    }

    public interface SnapshotHandler {
        void onSnapshot(SnapshotMsg snap);
    }
}
