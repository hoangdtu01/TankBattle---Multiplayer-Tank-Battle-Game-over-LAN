package com.game.net.protocol;

public enum MessageTypes {
    HELLO,          // client -> server (gửi tên)
    WELCOME,       // server -> client (gửi id)
    CREATE_ROOM,
    JOIN_ROOM,
    ROOM_UPDATE,
    REQUEST_ROOM_LIST,
    ROOM_LIST,
    READY,
    PEER_LIST,
    GAME_CONFIG,
    UDP_HELLO,
    UDP_ACK,
    INPUT,
    SNAPSHOT,
    START_GAME,
    REQUEST_ROOM_STATE,
    LEAVE_ROOM,
    ROOM_CLOSED
}
