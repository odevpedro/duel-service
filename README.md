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
| Game Engine   | ocgcore (C++) via JNI                |
| State Storage | In-memory (ConcurrentHashMap)        |
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
│       ├── messaging/             # DuelEventPublisher
│       ├── repository/            # InMemoryDuelRepository
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
```

---

## Como Rodar Localmente

### Pré-requisitos

- Java 21+
- Gradle 8+
- Biblioteca native ocgcore compilada (ver seção Biblioteca Nativa)

### Setup

```bash
# 1. Clone o repositório
git clone https://github.com/odevpedro/duel-service && cd duel-service

# 2. Compile e coloque a biblioteca nativa
# Ver seção abaixo

# 3. Execute
./gradlew bootRun
```

A API estará disponível em `http://localhost:8084`.

---

## Biblioteca Nativa

O duel-service carrega `ocgcore` como biblioteca nativa via JNI. O binário não é versionado — você deve compilarlocalmente.

**Compilando ocgcore**

```bash
git clone https://github.com/edo9300/ygopro-core
cd ygopro-core
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

**Colocando o binário**

| OS | Arquivo | Destino |
|---|---|---|
| Windows | `ocgcore.dll` | `src/main/resources/native/ocgcore.dll` |
| Linux | `libocgcore.so` | `src/main/resources/native/libocgcore.so` |
| macOS | `libocgcore.dylib` | `src/main/resources/native/libocgcore.dylib` |

O `OcgCoreLoader` extrai o binário do JAR em runtime e carrega via `System.load`.

---

## Testes

```bash
./gradlew test
```

---

## API — Endpoints Principais

| Método | Rota                        | Descrição                        | Auth |
|--------|-----------------------------|----------------------------------|------|
| POST   | `/api/duels`                | Criação de duelo                 |JWT |
| GET    | `/api/duels/{duelId}`       | Retorna estado do duelo          |JWT |
| WS     | `/ws`                       | Conexão WebSocket STOMP          |JWT |

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

## Fases do Duelo

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
- **Frontend (React)** → conecta via STOMP WebSocket para gameplay em tempo real

---

## Status do Projeto

```
[x] MVP — funcionalidades core implementadas
[x] v1.0 — autenticação JWT em WebSocket ✓
[x] v1.1 — manipulação de desconexão (timeout 3min + WO) ✓
[x] v2.0 — migração para Redis ✓
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