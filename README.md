# Ring Duel

> LAN Multiplayer Tank Battle Game developed using **Java** and **LibGDX**.

A fast-paced multiplayer arena battle game where players compete inside a circular arena using shooting, shields, dashes, and knockback mechanics. The game uses a hybrid networking model with **TCP for signaling** and **UDP for real-time gameplay synchronization**.

---

# Features

* Room Creation & Join System
* Ready Check System
* TCP Signaling Server
* UDP Gameplay Networking
* Host Authoritative Architecture
* Client Prediction & Reconciliation
* Shield System
* Dash Skill
* Knockback Combat Mechanics
* LAN Multiplayer Support

---

# Network Architecture

```text
Client
 ├── TCP Signaling
 └── UDP Gameplay

Server
 ├── Room Management
 ├── Match Start
 └── Player Synchronization
```

### Multiplayer Flow

```text
Player
   │
   ▼
TCP Signaling Server
   │
   ▼
Create / Join Room
   │
   ▼
Ready Check
   │
   ▼
Match Start
   │
   ▼
UDP Gameplay Synchronization
```

---

# Technologies

* Java
* LibGDX
* TCP Socket
* UDP DatagramSocket
* Git & GitHub

---

# Installation & Run

## Start Server

Run:

```text
SignalingServer.java
```

## Start Client

Run:

```text
Main.java
```

---

# How To Play

### Create Room

1. Enter your player name.
2. Select **Create Room**.
3. Share the Room ID with friends.

### Join Room

1. Enter your player name.
2. Select **Join Room**.
3. Choose a room from the lobby list.

### Start Match

* All players must press **READY**.
* The game starts automatically when everyone is ready.

---

# Controls

| Action            | Key |
| ----------------- | --- |
| Move Up           | W   |
| Move Down         | S   |
| Move Left         | A   |
| Move Right        | D   |
| Shoot             | J   |
| Dash Skill        | K   |
| Shield            | L   |
| Reload            | R   |
| Debug Information | F1  |

---

# Screenshots

<table>
<tr>
<td align="center">

### Main Menu

<img width="500" src="https://github.com/user-attachments/assets/5c5a61af-d0d2-4cdb-b2c0-4c518c3c5ade"/>

</td>
</tr>
</table>

<table>
<tr>
<td align="center">

### Lobby

<img width="500" src="https://github.com/user-attachments/assets/165fefa2-8a1d-4ed9-b19f-ca74e1c94d3c"/>

</td>

<td align="center">

### Room

<img width="500" src="https://github.com/user-attachments/assets/aed840a0-7fd9-4bb5-99dd-b5152f129e1d"/>

</td>
</tr>

<tr>
<td align="center">

### Ready System

<img width="500" src="https://github.com/user-attachments/assets/3f4446fd-9297-490f-a72d-a3d313e6f4dc"/>

</td>

<td align="center">

### Gameplay

<img width="500" src="https://github.com/user-attachments/assets/df910bd9-1be7-4ba6-b689-5be84c4947f2"/>

</td>
</tr>
</table>

---

# Project Structure (Overview)

```text
RingDuel
│
├── core/
│   ├── Arena.java
│   ├── Bullet.java
│   ├── Entity.java
│   ├── Player.java
│   ├── KnockbackSystem.java
│   ├── GameConfig.java
│   ├── GameSettings.java
│   └── GameState.java
│
├── gameplay/
│   ├── GameScreen.java
│   └── InputController.java
│
├── net/
│   ├── client/
│   │   ├── NetClient.java
│   │   ├── PeerConnection.java
│   │   └── HolePuncher.java
│   │
│   ├── server/
│   │   ├── SignalingServer.java
│   │   ├── Room.java
│   │   └── PlayerInfo.java
│   │
│   └── protocol/
│       ├── Messages.java
│       └── MessageTypes.java
│
├── ui/
│   ├── hud/
│   │   └── HUDRenderer.java
│   │
│   └── screens/
│       ├── MainMenuScreen.java
│       ├── NameInputScreen.java
│       ├── LobbyScreen.java
│       ├── RoomScreen.java
│       ├── SettingScreen.java
│       └── HowToPlayScreen.java
│
├── Main.java
└── RingDuelGame.java
```

---

# Future Improvements

* Dedicated Server Support
* Matchmaking System
* Multiple Maps
* Additional Skills & Weapons
* Ranking System
* Spectator Mode

---

# Author

**TRẦN VĂN HOÀNG**
Vietnam - VKU (Vietnam-Korea University of Information and Communication Technology)

```
```
