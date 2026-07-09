# duel-service

> Motor de duelos Yu-Gi-Oh! em tempo real dibangun dengan Spring Boot e WebSocket (STOMP), alimentado pelo motor C++ ocgcore via JNI. Parte do ecossistema de microsserviços yu-gi-oh-collections.

---

## Sobre o Projeto

API REST e WebSocket para gerenciamento de duelos Yu-Gi-Oh! em tempo real, integrando o frontend React com o motor C++ ocgcore via JNI. Gerencia ciclo de vida completo do duelo (criação a game over), comunicação bidirecional em tempo real e sincronização de estado entre jogadores.

---

## Stack & Arquitetura

| Camada        | Tecnologia                          |
|---------------|--------------------------------------|
| Runtime       | Java 21                              |
| Framework     | Spring Boot 3.2                      |
| Real-time     | WebSocket + STOMP (SockJS)           |
| Game Engine   | ygopro-core (C++) via JNI bridge, com fallback Stub |
| State Storage | Redis com fallback InMemory          |
| Build        | Gradle                               |
| Testes        | JUnit / Spring Boot Test              |

> Padrão arquitetural: **Hexagonal Architecture (Ports & Adapters)** com separação em camadas `adapter → application → domain`.

---

## Estrutura de Pastas

```
src/main/java/com/odevpedro/yugiohcollections/duel/
├── adapter/
│   ├── in/
│   │   ├── rest/                  # DuelController (POST /api/duels)
│   │   └── websocket/             # DuelActionHandler, SessionHandler
│   │       └── config/            # WebSocketConfig (STOMP)
│   └── out/
│       ├── messaging/             # DuelEventPublisher + eventos Kafka
│       ├── repository/            # RedisDuelRepository + InMemory fallback
│       └── ocgcore/               # OcgCoreBridge, OcgCoreAdapter, OcgCoreLoader
│
├── application/
│   ├── dto/                       # CreateDuelRequest, DuelResponse, DuelActionDTO
│   ├── mapper/                    # DuelMapper
│   └── service/                   # interfaces e implementações
│
├── config/                        # SecurityConfig, GlobalExceptionHandler, OcgCoreConfig
│
    └── domain/
        ├── model/                     # DuelState, Player, Zone, Card
        │   └── enums/                 # Phase, GameStatus, CardType, ZoneType, CardPosition
        └── port/                      # DuelRepositoryPort, DuelEventPublisherPort, OcgCorePort

native/
└── ocgcore-bridge/                    # JNI bridge C++ (CMake + ygopro-core)
```

---

## Como Rodar Localmente

### Pré-requisitos

- Java 21+
- Gradle 8+
- CMake 3.20+ (para build da lib nativa)
- Compilador C++20

### Setup rápido (com fallback Stub)

```bash
# Sobe apenas o Redis (opcional, para desenvolvimento)
docker compose up -d

# Roda o serviço no perfil dev (usa OcgCoreStub, sem lib nativa)
./gradlew bootRun
```

A API estará disponível em `http://localhost:8084`.

### Setup com biblioteca nativa (produção)

```bash
# Compila o bridge JNI + ygopro-core
./gradlew fullBuildNative

# Empacota e executa com lib nativa
./gradlew bootJar
java -jar build/libs/duel-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Biblioteca Nativa (ocgcore)

O duel-service integra o motor C++ [ygopro-core](https://github.com/edo9300/ygopro-core)
via JNI. A comunicação é feita por JSON — o bridge C++ (em `native/ocgcore-bridge/`)
converte as chamadas JNI em chamadas para a API C do ygopro-core.

### Arquitetura de integração

```
┌──────────────────┐     JNI      ┌──────────────────┐     C API     ┌──────────────┐
│  OcgCoreAdapter  │ ──────────▶  │  libocgcore.so   │ ────────────▶ │  ygopro-core │
│  (Java/Spring)   │ ◀──────────  │  (JNI bridge)    │ ◀──────────── │  (C++ engine)│
└──────────────────┘              └──────────────────┘               └──────────────┘
```

### Fallback automático

Se a biblioteca nativa não estiver disponível, o `OcgCoreStub` (Java puro)
assume o processamento. A decisão é automática e transparente para o
restante do sistema.

### Compilando a biblioteca nativa

```bash
# Via Gradle (automático — baixa ygopro-core via CMake FetchContent)
./gradlew fullBuildNative

# Ou manualmente
cd native/ocgcore-bridge
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

O CMakeLists.txt em `native/ocgcore-bridge/` baixa e compila três dependências via FetchContent:
- **Lua 5.4** (`v5.4.7`) — compilado como lib estática (`lua54`) para o script reader do ygopro-core
- **ygopro-core** (`master`) — compilado como lib estática (`ocgcore-core`) que linka contra Lua
- **nlohmann_json** (`v3.11.3`) — header-only para serialização JSON dentro do bridge

O bridge C++ (`ocgcore_bridge.cpp`) declara as funções JNI que recebem JSON do Java, mantém instâncias persistentes de `OCG_Duel` por `duelId` em um mapa global, alimentam cartas no motor apenas na primeira chamada, executam o loop de processamento (`OCG_DuelProcess` / `OCG_DuelGetMessage` / `OCG_DuelSetResponse`), consultam o campo via `OCG_DuelQueryField` e retornam um JSON com o resultado do motor (EngineResult + field data). Duas chamadas consecutivas para o mesmo duelo reutilizam a mesma instância do motor C++. A lib gerada tem ~3,2 MB.

O binário gerado é copiado para `src/main/resources/native/`.

### Fluxo de processamento Java
O `OcgCoreAdapter` serializa o `DuelState` e o `DuelActionDTO` em JSON, chama o bridge JNI, e então parseia a resposta como `OcgCoreBridgeResponse` (que contém o `EngineResult` inner class). O método `applyEngineResult()` extrai turno, fase, LP, jogador ativo, game over e winner do `EngineResult` e aplica ao `DuelState`, atualizando também os campos de campo (field data) retornados pelo motor C++.

### Tabela de binários

| OS      | Arquivo                  | Destino                                    |
|---------|--------------------------|--------------------------------------------|
| Linux   | `libocgcore.so`          | `src/main/resources/native/libocgcore.so`  |
| macOS   | `libocgcore.dylib`       | `src/main/resources/native/libocgcore.dylib` |
| Windows | `ocgcore.dll`            | `src/main/resources/native/ocgcore.dll`    |

### Empacotando no JAR

```bash
./gradlew fullBuildNative bootJar
```

O `OcgCoreLoader` extrai o binário do classpath em runtime e carrega via `System.load`.

---

## Testes

```bash
./gradlew test
```

---

## API — Endpoints Principais

| Método | Rota                        | Descrição                        | Auth |
|--------|-----------------------------|----------------------------------|------|
| POST   | `/api/duels`                | Criação de duelo (com deckIds)  |JWT |
| GET    | `/api/duels/{duelId}`       | Retorna estado do duelo          |JWT |
| GET    | `/api/duels/history`        | Lista últimos 100 duelos         |JWT |
| GET    | `/api/duels/history/{duelId}` | Detalhes de um duelo         |JWT |
| GET    | `/api/duels/history/player/{playerId}` | Histórico de um jogador |JWT |
| WS     | `/ws`                       | Conexão WebSocket STOMP (SockJS) |JWT |
| WS     | `/ws-raw`                   | Conexão WebSocket STOMP (raw)    |JWT |

**Tópicos WebSocket**

| Tópico | Descrição |
|--------|------------|
| `/topic/duel/{duelId}` | Eventos de estado do duelo |
| `/topic/duel/{duelId}/over` | Fim de duelo com vencedor |

**Destinos de ação**

| Destino | Descrição |
|--------|------------|
| `/app/duel.action` | Executar ação (SUMMON, ATTACK, SPELL, SET) |
| `/app/duel.phase` | Avançar fase |

---

## Fases do Duelo (bridge nativo)

```
DRAW → STANDBY → MAIN_1 → BATTLE → MAIN_2 → END → (próximo turno)
```

| Fase | Ações permitidas |
|---|---|
| `MAIN_1`, `MAIN_2` | `SUMMON`, `SPELL`, `SET` |
| `BATTLE` | `ATTACK` |
| Outras | nenhuma |

Validação de fase e processamento de ações são delegados ao `ocgcore`.

---

## Documentação Técnica

| Documento                                         | Descrição                                    |
|---------------------------------------------------|----------------------------------------------|
| [Fluxos de Funcionalidades](./docs/system-feature-flows.md) | Fluxo interno de cada feature      |
| [Backlog](./backlog.md)                           | Status de desenvolvimento do projeto         |

---

## Integração

Este serviço integra com o ecossistema `yu-gi-oh-collections`:

- **community-service** → cria duelos via `POST /api/duels` após matcher de jogadores
- **auth-service** → validação JWT em conexões WebSocket
- **deck-service** → carrega decks dos jogadores ao criar duelo
- **Frontend (React)** → conecta via STOMP WebSocket para gameplay em tempo real

---

## Status do Projeto

```
[x] MVP — funcionalidades core implementadas
[x] v1.0 — autenticação JWT em WebSocket ✓
[x] v1.1 — manipulação de desconexão (timeout 3min + WO) ✓
[x] v2.0 — migração para Redis ✓
[x] v3.0 — duel history persistence ✓
[x] v3.1 — deck integration via Feign ✓
[x] v3.2 — bridge nativo stateful (C++ ygopro-core via JNI) ✓
[x] v3.3 — sincronização de posições via field_data (NATIVE-009) ✓
[x] v3.4 — desenho da mão inicial pelo motor C++ (NATIVE-010) ✓
```

---

## Contribuindo

1. Fork o repositório
2. Crie uma branch: `git checkout -b feature/minha-feature`
3. Commit suas mudanças: `git commit -m 'feat: adiciona minha feature'`
4. Push: `git push origin feature/minha-feature`
5. Abra um Pull Request descrevendo o que foi feito

> Siga o padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/).

---

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](./LICENSE) para mais informações.

---

<p align="center">
  Feito com foco em qualidade por <a href="https://github.com/odevpedro">@odevpedro</a>
</p>
