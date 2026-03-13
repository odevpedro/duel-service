# duel-service

Real-time Yu-Gi-Oh! duel engine built with **Spring Boot** and **WebSocket (STOMP)**, powered by the **ocgcore** C++ engine via JNI. Part of the `yu-gi-oh-collections` microservices ecosystem.

---

## Overview

The `duel-service` manages the full lifecycle of a Yu-Gi-Oh! duel — from creation to game over. It bridges the React frontend and the `ocgcore` C++ engine, handling real-time bidirectional communication, session management, and state synchronization between players.

```
React (Player A) ──┐
                   ├──► WebSocket/STOMP ──► duel-service ──► JNI ──► ocgcore (C++)
React (Player B) ──┘
```

Duels are initiated by the `community-service` when two players are matched via geolocation. The `duel-service` receives a `POST /api/duels` with both player IDs, creates the duel state, and opens the WebSocket session for real-time gameplay.

---

## Architecture

This service follows **Hexagonal Architecture (Ports & Adapters)**, consistent with the rest of the `yu-gi-oh-collections` monorepo.

```
src/main/java/com/odevpedro/yugiohcollections/duel/
│
├── adapter/
│   ├── in/
│   │   ├── rest/                  # DuelController (POST /api/duels)
│   │   └── websocket/             # DuelActionHandler, SessionHandler
│   │       └── config/            # WebSocketConfig (STOMP)
│   └── out/
│       ├── messaging/             # DuelEventPublisher (SimpMessagingTemplate)
│       ├── repository/            # InMemoryDuelRepository (ConcurrentHashMap)
│       └── ocgcore/               # OcgCoreBridge, OcgCoreAdapter, OcgCoreLoader
│
├── application/
│   ├── dto/                       # CreateDuelRequest, DuelResponse, DuelActionDTO
│   ├── mapper/                    # DuelMapper
│   └── service/
│       ├── DuelApplicationService # interface
│       ├── ActionService          # interface
│       ├── PhaseService           # interface
│       └── Impl/                  # implementations
│
├── config/                        # SecurityConfig, GlobalExceptionHandler, OcgCoreConfig
│
└── domain/
    ├── model/                     # DuelState, Player, Zone, Card
    │   └── enums/                 # Phase, GameStatus, CardType, ZoneType, CardPosition
    └── port/                      # DuelRepositoryPort, DuelEventPublisherPort, OcgCorePort
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Real-time | WebSocket + STOMP (SockJS) |
| Game engine | ocgcore (C++) via JNI |
| State storage | In-memory (`ConcurrentHashMap`) |
| Build | Gradle |
| Java | 21 |

---

## Prerequisites

- Java 21+
- Gradle 8+
- ocgcore compiled for your OS (see [Native Library](#native-library))

---

## Getting Started

**1. Clone the repository**

```bash
git clone https://github.com/odevpedro/duel-service
cd duel-service
```

**2. Build and place the native library**

See the [Native Library](#native-library) section below.

**3. Run**

```bash
./gradlew bootRun
```

The service starts on port `8084`.

---

## Native Library

The `duel-service` loads `ocgcore` as a native library at startup. The binary is not versioned — you must compile it locally and place it in `src/main/resources/native/`.

**Building ocgcore**

```bash
git clone https://github.com/edo9300/ygopro-core
cd ygopro-core
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

**Placing the binary**

| OS | File | Destination |
|---|---|---|
| Windows | `ocgcore.dll` | `src/main/resources/native/ocgcore.dll` |
| Linux | `libocgcore.so` | `src/main/resources/native/libocgcore.so` |
| macOS | `libocgcore.dylib` | `src/main/resources/native/libocgcore.dylib` |

The `OcgCoreLoader` extracts the binary from the JAR at runtime into a temp directory and loads it via `System.load` — no external configuration required.

---

## WebSocket API

**Connection endpoint**

```
ws://localhost:8084/ws
```

SockJS fallback enabled. Connect using a STOMP client.

**Subscribing to duel events**

```javascript
stompClient.subscribe(`/topic/duel/${duelId}`, (message) => {
    const state = JSON.parse(message.body); // DuelState
});

stompClient.subscribe(`/topic/duel/${duelId}/over`, (message) => {
    const winnerId = message.body;
});
```

**Sending actions**

```javascript
// Perform a game action
stompClient.publish({
    destination: '/app/duel.action',
    body: JSON.stringify({
        duelId: 'abc-123',
        actionType: 'SUMMON', // SUMMON | ATTACK | SPELL | SET
        cardId: '42',
        zoneIndex: 2
    })
});

// Advance the phase
stompClient.publish({
    destination: '/app/duel.phase',
    body: JSON.stringify({ duelId: 'abc-123' })
});
```

---

## REST API

**Create a duel**

```http
POST /api/duels
Content-Type: application/json

{
  "playerAId": "uuid-player-1",
  "playerBId": "uuid-player-2"
}
```

```http
HTTP/1.1 201 Created

{
  "duelId": "duel-abc-123",
  "playerAId": "uuid-player-1",
  "playerBId": "uuid-player-2",
  "currentPhase": "DRAW",
  "status": "IN_PROGRESS",
  "turnNumber": 1,
  "activePlayerId": "uuid-player-1"
}
```

**Get duel state**

```http
GET /api/duels/{duelId}
```

---

## Duel Phases

```
DRAW → STANDBY → MAIN_1 → BATTLE → MAIN_2 → END → (next turn)
```

| Phase | Allowed actions |
|---|---|
| `MAIN_1`, `MAIN_2` | `SUMMON`, `SPELL`, `SET` |
| `BATTLE` | `ATTACK` |
| Others | none |

All phase validation and action processing is delegated to `ocgcore`.

---

## Integration

This service integrates with the `yu-gi-oh-collections` ecosystem as follows:

- **community-service** → creates duels via `POST /api/duels` after matching players
- **auth-service** → JWT validation on WebSocket connections (via `SecurityConfig`)
- **Frontend (React)** → connects via STOMP WebSocket for real-time gameplay

---

## Running Tests

```bash
./gradlew test
```

---

## Configuration

```yaml
# src/main/resources/application.yml
server:
  port: 8084

spring:
  application:
    name: duel-service
```

For environment-specific overrides, create `application-local.yml` (already in `.gitignore`).

---

## Roadmap

- [ ] JWT authentication on WebSocket handshake
- [ ] Disconnect handling — notify opponent and pause duel
- [ ] Redis migration for persistent duel state
- [ ] Card database integration with `card-creator-service`
- [ ] Duel history and result persistence
