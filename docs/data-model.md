# Data Model — duel-service

> Documentacao do modelo de dados do duel-service.
> Ultima atualizacao: 2026-07-09 — field sync (NATIVE-009), desenho mao pelo C++ (NATIVE-010)

---

## Indice

> Atualizado em: 2026-07-09 — field sync (NATIVE-009), desenho mao pelo C++ (NATIVE-010)

- [Entidades](#entidades)
  - [DuelState](#duelstate)
  - [Player](#player)
  - [Card](#card)
  - [Zone](#zone)
  - [DuelHistoryEntity](#duelhistoryentity)
- [Enums](#enums)
  - [Phase](#phase)
  - [GameStatus](#gamestatus)
  - [CardType](#cardtype)
  - [CardPosition](#cardposition)
  - [ZoneType](#zonetype)
- [Relacionamentos](#relacionamentos)
- [DTOs](#dtos)

---

## Entidades

### DuelState

Estado vivo de um duelo. Persistido em Redis (producao) ou InMemory (dev).

| Campo | Tipo | Descricao |
|-------|------|-----------|
| duelId | String | ID unico do duelo (UUID) |
| playerAId | String | ID do jogador A |
| playerBId | String | ID do jogador B |
| playerADeckId | Long | ID do deck usado pelo jogador A |
| playerBDeckId | Long | ID do deck usado pelo jogador B |
| currentPhase | Phase | Fase atual do turno |
| turnNumber | int | Numero do turno atual (inicia em 1) |
| activePlayerId | String | ID do jogador que esta na vez |
| playerA | Player | Dados do jogador A (deck, mao, LP, zonas) |
| playerB | Player | Dados do jogador B |
| status | GameStatus | Status atual do duelo |
| winnerId | String | ID do vencedor (null se em andamento ou empate) |
| victoryType | String | Tipo de vitoria: NORMAL, WO, DRAW, etc. |
| duelType | String | Tipo de duelo: CASUAL, RANKED, FRIENDLY, etc. |
| createdAt | LocalDateTime | Momento da criacao |
| updatedAt | LocalDateTime | Ultima atualizacao |
| firstTurn | boolean | Flag para primeiro turno (skip draw) |
| disconnectedPlayerId | String | ID do jogador que desconectou (null se nenhum) |
| disconnectedAt | LocalDateTime | Timestamp da desconexao |
| version | long | Versao incremental do estado para detectar divergencia |

**Metodos:**
- `getOpponent(String playerId)` — retorna o Player oponente
- `getActivePlayer()` — retorna o Player ativo
- `hasDisconnected()` — true se alguem esta desconectado

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/DuelState.java`

---

### Player

Estado individual de cada jogador dentro do duelo.

| Campo | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| playerId | String | — | ID do jogador |
| lifePoints | int | 8000 | Pontos de vida (minimo 0) |
| hand | List<Card> | [] | Cartas na mao |
| deck | List<Card> | [] | Cartas no deck (o que resta) |
| graveyard | List<Card> | [] | Cartas no cemiterio |
| banished | List<Card> | [] | Cartas banidas |
| extraDeck | List<Card> | [] | Cartas do Extra Deck |
| sideDeck | List<Card> | [] | Cartas do Side Deck |
| monsterZones | List<Zone> | [] | Zonas de monstro (5 slots) |
| spellTrapZones | List<Zone> | [] | Zonas de magia/armadilha (5 slots) |

**Metodos:**
- `setLifePoints(int)` — clamp em 0 (nunca negativo)
- `takeDamage(int)` — reduz LP com validacao de valor negativo
- `gainLife(int)` — aumenta LP com validacao de valor negativo
- `isAlive()` — true se lifePoints > 0

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/Player.java`

---

### Card

Representacao de uma carta dentro do duelo.

| Campo | Tipo | Descricao |
|-------|------|-----------|
| cardId | String | ID da carta (vindo do card-service) |
| name | String | Nome da carta |
| imageUrl | String | URL da imagem da carta |
| atk | int | Pontos de ataque |
| def | int | Pontos de defesa |
| level | int | Nivel/N: 1-12 (monstros) |
| type | CardType | Tipo: MONSTER, SPELL, TRAP |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/Card.java`

---

### Zone

Slot no campo de jogo que pode conter uma carta.

| Campo | Tipo | Descricao |
|-------|------|-----------|
| index | int | Indice da zona (0-4) |
| type | ZoneType | Tipo: MONSTER, SPELL_TRAP, FIELD |
| card | Card | Carta ocupando a zona (null se vazia) |
| position | CardPosition | Posicao da carta: ATTACK, DEFENSE_FACE_UP, DEFENSE_FACE_DOWN |

**Metodos:**
- `isEmpty()` — true se card == null

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/Zone.java`

---

### DuelHistoryEntity

Registro persistente de um duelo finalizado. Usa JPA/H2 (dev) ou PostgreSQL (producao).

| Campo | Tipo | Constraints | Descricao |
|-------|------|-------------|-----------|
| id | Long | PK, AUTO_INCREMENT | ID interno |
| duelId | String | UNIQUE, NOT NULL | ID do duelo |
| playerAId | String | NOT NULL | ID do jogador A |
| playerBId | String | NOT NULL | ID do jogador B |
| playerADeckId | Long | nullable | ID do deck usado pelo jogador A |
| playerBDeckId | Long | nullable | ID do deck usado pelo jogador B |
| winnerId | String | nullable | ID do vencedor (null = empate) |
| loserId | String | nullable | ID do perdedor |
| playerAFinalLp | Integer | — | LP final do jogador A |
| playerBFinalLp | Integer | — | LP final do jogador B |
| turnCount | Integer | — | Numero de turnos |
| duelType | String | — | Tipo de duelo |
| victoryType | String | — | Tipo de vitoria |
| result | String | — | COMPLETED ou DRAW |
| startedAt | LocalDateTime | NOT NULL | Inicio do duelo |
| finishedAt | LocalDateTime | — | Fim do duelo |
| durationSeconds | Long | — | Duracao em segundos |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/adapter/out/persistence/entity/DuelHistoryEntity.java`
**Migration:** `src/main/resources/db/migration/V1__init_schema.sql`

---

## Enums

### Phase

| Valor | Descricao |
|-------|-----------|
| DRAW | Fase de compra |
| STANDBY | Fase de espera (efeitos continuos) |
| MAIN_1 | Primeira fase principal (invocar, ativar magia) |
| BATTLE | Fase de batalha (atacar) |
| MAIN_2 | Segunda fase principal |
| END | Fase final |

**Ciclo:** DRAW → STANDBY → MAIN_1 → BATTLE → MAIN_2 → END → (repete)

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/enums/Phase.java`

---

### GameStatus

| Valor | Descricao |
|-------|-----------|
| WAITING | Aguardando jogadores |
| IN_PROGRESS | Duelo em andamento |
| FINISHED | Duelo finalizado |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/enums/GameStatus.java`

---

### CardType

| Valor | Descricao |
|-------|-----------|
| MONSTER | Carta de monstro |
| SPELL | Carta de magia |
| TRAP | Carta de armadilha |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/enums/CardType.java`

---

### CardPosition

| Valor | Descricao |
|-------|-----------|
| ATTACK | Posicao de ataque (face-up) |
| DEFENSE_FACE_UP | Posicao de defesa (face-up) |
| DEFENSE_FACE_DOWN | Posicao de defesa (face-down) |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/enums/CardPosition.java`

---

### ZoneType

| Valor | Descricao |
|-------|-----------|
| MONSTER | Zona de monstro |
| SPELL_TRAP | Zona de magia/armadilha |
| FIELD | Zona de campo |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/domain/model/enums/ZoneType.java`

---

## Relacionamentos

```
DuelState
 ├── playerA ──→ Player
 │               ├── hand ──→ Card[]
 │               ├── deck ──→ Card[]
 │               ├── graveyard ──→ Card[]
 │               ├── banished ──→ Card[]
 │               ├── extraDeck ──→ Card[]
 │               ├── sideDeck ──→ Card[]
 │               └── monsterZones ──→ Zone[]
 │               └── spellTrapZones ──→ Zone[]
 │                                └── card ──→ Card
 │                                └── position ──→ CardPosition
 └── playerB ──→ Player (mesma estrutura)

DuelHistoryEntity (tabela relacional, independente)
```

---

## DTOs

### OcgCoreBridgeResponse

DTO para parsear a resposta JSON do bridge JNI C++. Usado por `OcgCoreAdapter.applyEngineResult()` para mesclar o resultado do motor no `DuelState`.

| Campo | Tipo | Descricao |
|-------|------|-----------|
| duelId | String | ID do duelo (ecoado do estado de entrada) |
| turnNumber | int | Numero do turno processado |
| currentPhase | String | Fase atual como string ("MAIN_1", "BATTLE", etc.) |
| status | String | Status ("IN_PROGRESS" ou "FINISHED") |
| engine | EngineResult | Resultado interno do motor C++ (ver abaixo) |

### EngineResult (inner class de OcgCoreBridgeResponse)

Resultado processado pelo motor ygopro-core. Extraido do bloco `engine` da resposta JSON do bridge.

| Campo | Tipo | Descricao |
|-------|------|-----------|
| turn | int | Turno atual (vindo de `MSG_NEW_TURN`) |
| phase | int | Codigo numerico da fase (0x01=DRAW, 0x02=STANDBY, 0x04=MAIN_1, 0x08=BATTLE_START, 0x10=BATTLE_STEP, 0x20=DAMAGE, 0x40=DAMAGE_CAL, 0x80=BATTLE, 0x100=MAIN_2, 0x200=END) |
| turnPlayer | int | Indice do jogador ativo (0 ou 1) |
| lp0 | int | LP do jogador 0 |
| lp1 | int | LP do jogador 1 |
| gameOver | boolean | Se o duelo terminou (recebeu `MSG_WIN` ou `MSG_RETRY`) |
| winnerPlayer | int | Indice do vencedor (0 ou 1, 0 se empate) |
| winReason | int | Razao da vitoria (1=NORMAL) |
| field | Object | Objeto JSON do field data (zonas, cadeia) via `OCG_DuelQueryField` |

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/adapter/out/ocgcore/OcgCoreBridgeResponse.java`

---

### CreateDuelRequest

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| playerAId | String | Sim | ID do jogador A |
| playerBId | String | Sim | ID do jogador B |
| playerADeckId | Long | Nao | ID do deck do jogador A |
| playerBDeckId | Long | Nao | ID do deck do jogador B |

### DuelResponse

| Campo | Tipo | Descricao |
|-------|------|-----------|
| duelId | String | ID do duelo criado |
| playerAId | String | ID do jogador A |
| playerBId | String | ID do jogador B |
| currentPhase | String | Fase inicial (DRAW) |
| status | String | Status (IN_PROGRESS) |
| turnNumber | int | Turno (1) |
| activePlayerId | String | Jogador que comeca |
| winnerId | String | Vencedor, quando o duelo terminou |

### DuelActionDTO

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| duelId | String | Sim | ID do duelo |
| actionType | String | Sim | SUMMON, ATTACK, SPELL, SET |
| cardId | String | Condicional | ID da carta alvo |
| targetId | String | Nao | ID da carta alvo (ataque) |
| zoneIndex | Integer | Nao | Indice da zona alvo |

### PhaseChangeDTO

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| duelId | String | Sim | ID do duelo |

### DuelHistoryResponse

| Campo | Tipo | Descricao |
|-------|------|-----------|
| id | Long | ID interno |
| duelId | String | ID do duelo |
| playerAId | String | Jogador A |
| playerBId | String | Jogador B |
| winnerId | String | Vencedor |
| loserId | String | Perdedor |
| playerADeckId | Long | Deck usado pelo jogador A |
| playerBDeckId | Long | Deck usado pelo jogador B |
| playerAFinalLp | Integer | LP final A |
| playerBFinalLp | Integer | LP final B |
| turnCount | Integer | Turnos |
| duelType | String | Tipo de duelo |
| victoryType | String | Tipo de vitoria |
| result | String | COMPLETED / DRAW |
| startedAt | String | ISO timestamp |
| finishedAt | String | ISO timestamp |
| durationSeconds | Long | Duracao |

---

## Diagrama de Estados do Duelo

```
                ┌──────────┐
                │  CRIADO  │
                │  (DRAW)  │
                └────┬─────┘
                     │
                     ▼
             ┌───────────────┐
        ┌───▶│  IN_PROGRESS  │◀──────────┐
        │    │  (ciclo de    │           │
        │    │   fases)      │           │
        │    └───────┬───────┘           │
        │            │                   │
        │            │ (ambos vivos)     │
        │            ▼                   │
        │    ┌───────────────┐           │
        │    │  FIM DE TURNO │───────────┘
        │    │  (END → DRAW) │
        │    └───────────────┘
        │
        │ (alguem morre)
        ▼
 ┌──────────────┐
 │   FINISHED   │
 │ (winnerId ou │
 │  null= DRAW) │
 └──────────────┘
```
