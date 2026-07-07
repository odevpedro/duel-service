# Backlog — duel-service

> Registro vivo do progresso do projeto. Atualizado a cada mudanca de estado de uma funcionalidade.
> **Ultima atualizacao:** 2026-07-07

---

## Sobre o Projeto

API REST e WebSocket para gerenciamento de duelos Yu-Gi-Oh! em tempo real, integrando o frontend React com o motor C++ ocgcore via JNI.

**Versao atual:** `0.0.1-SNAPSHOT`
**Repositorio:** [github.com/odevpedro/duel-service](https://github.com/odevpedro/duel-service)
**Stack principal:** Java 21 + Spring Boot 3.2 + WebSocket/STOMP + JNI + ocgcore + Redis

---

## Legenda

| Simbolo | Significado |
|---------|-------------|
| `[ ]`   | Pendente |
| `[~]`   | Em andamento |
| `[x]`   | Concluido |
| `P0`    | Critico — bloqueia outras features |
| `P1`    | Alta prioridade |
| `P2`    | Media prioridade |
| `P3`    | Melhoria / nice-to-have |

---

## Em Andamento

> Nenhuma feature em andamento.

---

## Pendentes

### FASE 0 — Criticos (impedem compilacao/execucao)

---

#### `[ ]` BUG-002 — DuelEventPublisher.publishGameOver() duplicado

**Descricao:** O metodo `publishGameOver()` esta declarado duas vezes no mesmo arquivo (linhas 22-25 e 46-51), causando erro de compilacao.

**Arquivo:** `src/main/java/com/odevpedro/yugiohcollections/duel/adapter/out/messaging/DuelEventPublisher.java`

**Checklist:**
- [ ] Remover a segunda declaracao do metodo (linhas 46-51)
- [ ] Verificar se o arquivo compila com `./gradlew compileJava`
- [ ] Verificar se os testes que chamam `publishGameOver()` continuam passando

**Criterio de aceitacao:** `./gradlew build` passa sem erros.

**Estimativa:** XS

---

#### `[ ]` BUG-003 — Coordenada Maven do OpenFeign invalida

**Descricao:** `build.gradle` linha 22 usa `implementation 'org.springframework.cloud/openfeign'` — formato de coordenada Maven incorreto. O correto e adicionar o BOM do Spring Cloud e usar `org.springframework.cloud:spring-cloud-starter-openfeign`.

**Arquivo:** `build.gradle`

**Checklist:**
- [ ] Adicionar BOM Spring Cloud no `dependencyManagement`:
  ```groovy
  dependencyManagement {
      imports {
          mavenBom "org.springframework.cloud:spring-cloud-dependencies:2022.0.4"
      }
  }
  ```
- [ ] Substituir `implementation 'org.springframework.cloud/openfeign'` por `implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'`
- [ ] Executar `./gradlew build` para validar

**Criterio de aceitacao:** Resolucao de dependencia do OpenFeign funciona, `./gradlew build` passa.

**Estimativa:** XS

---

#### `[ ]` BUG-004 — Conflito de beans DuelRepositoryPort em perfil dev

**Descricao:** `InMemoryDuelRepository` tem `@Profile("dev")`, mas `RedisDuelRepository` nao tem restricao de profile. No perfil `dev`, ambos estao ativos como implementacoes de `DuelRepositoryPort`, causando `NoUniqueBeanDefinitionException`.

**Solucao 1 (recomendada):** Adicionar `@Profile("!dev")` no `RedisDuelRepository`.

**Solucao 2:** Usar `@Primary` em um deles.

**Checklist:**
- [ ] Adicionar `@Profile("!dev")` no `RedisDuelRepository`
- [ ] Verificar se a aplicacao sobe em perfil `dev` sem erro: `./gradlew bootRun --args='--spring.profiles.active=dev'`
- [ ] Verificar se `InMemoryDuelRepository` e injetado corretamente em dev

**Criterio de aceitacao:** Aplicacao sobe em perfil `dev` sem conflito de beans. Em perfil `prod` (ou ausente), `RedisDuelRepository` e usado.

**Estimativa:** S

---

#### `[ ]` BUG-005 — V1__init_schema.sql vazio

**Descricao:** O arquivo de migration Flyway em `src/main/resources/db/migration/V1__init_schema.sql` esta vazio (0 bytes). A tabela `duel_history` nao e criada.

**Observacao:** Atualmente o `application.yml` usa `ddl-auto: update` do Hibernate, que cria a tabela automaticamente. Quando migrar para PostgreSQL em producao, a migration sera necessaria.

**Checklist:**
- [ ] Popular o arquivo com o DDL da tabela `duel_history`:
  ```sql
  CREATE TABLE IF NOT EXISTS duel_history (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      duel_id VARCHAR(255) NOT NULL UNIQUE,
      player_a_id VARCHAR(255) NOT NULL,
      player_b_id VARCHAR(255) NOT NULL,
      winner_id VARCHAR(255),
      loser_id VARCHAR(255),
      player_a_final_lp INTEGER,
      player_b_final_lp INTEGER,
      turn_count INTEGER,
      duel_type VARCHAR(50),
      result VARCHAR(50),
      started_at TIMESTAMP NOT NULL,
      finished_at TIMESTAMP,
      duration_seconds BIGINT
  );
  ```

**Criterio de aceitacao:** Flyway consegue aplicar a migration em um banco H2 limpo.

**Estimativa:** XS

---

#### `[ ]` BUG-006 — Endpoints REST /api/duels/ sem autenticacao JWT

**Descricao:** `SecurityConfig` libera `/api/duels/**` com `permitAll()`, mas a protecao WebSocket so existe no `JwtChannelInterceptor`. Os endpoints REST (POST /api/duels, GET /api/duels/{id}, etc.) podem ser chamados sem token.

**Impacto:** Qualquer pessoa pode criar duelos e ver estados, mesmo sem estar logada.

**Checklist:**
- [ ] Remover `/api/duels/**` do `permitAll()` no `SecurityConfig`
- [ ] Adicionar rota de health check (ex: `/actuator/health`) como unica excecao
- [ ] Adicionar dependencia `spring-boot-starter-actuator` (opcional)
- [ ] Verificar que o fluxo de login do frontend envia JWT no header `Authorization: Bearer <token>` ao chamar REST endpoints
- [ ] Testar com `curl -X POST /api/duels` sem token → 401/403

**Criterio de aceitacao:** Qualquer chamada a `/api/duels/**` sem token valido retorna 401. Chamadas com token valido funcionam.

**Estimativa:** S

---

#### `[ ]` DS-001 — Criar docs/data-model.md

**Descricao:** O CLAUDE.md exige o arquivo `docs/data-model.md` com a documentacao do modelo de dados, mas ele nao existe.

**Checklist:**
- [ ] Criar `docs/data-model.md` seguindo o template em `~/Documentos/repos/claude-config/data-model-template.md`
- [ ] Documentar todas as entidades: `DuelState`, `Player`, `Card`, `Zone`, `DuelHistoryEntity`
- [ ] Documentar todos os enums: `Phase`, `GameStatus`, `CardType`, `CardPosition`, `ZoneType`
- [ ] Incluir diagrama de relacoes entre entidades

**Criterio de aceitacao:** Arquivo criado com todas as entidades e enums do codigo real documentados.

**Estimativa:** S

---

### FASE 1 — Gameplay (motor do duelo funcional para um jogo jogavel)

---

#### `[ ]` GAME-001 — Setup inicial: embaralhar deck e distribuir mao inicial

**Descricao:** `DuelApplicationServiceImpl.createDuel()` cria os jogadores com `lifePoints=8000` e carrega os decks, mas nao embaralha nem distribui a mao inicial de 5 cartas. Sem isso, o jogador comeca sem cartas na mao e nao consegue jogar nada.

**Onde:** `DuelApplicationServiceImpl.createDuel()`

**Checklist:**
- [ ] Implementar metodo `shuffleDeck(List<Card> deck)` — usar `Collections.shuffle()` com `ThreadLocalRandom`
- [ ] Embaralhar o deck de cada jogador apos carregar do deck-service
- [ ] Distribuir 5 cartas do topo do deck para a mao (`hand`) de cada jogador
- [ ] Remover as 5 cartas do deck apos distribuir
- [ ] Ajustar `turnNumber` para iniciar em 1
- [ ] Escrever teste unitario para validar: deck inicial tem 35-55 cartas (dependendo do total), hand tem 5 cartas
- [ ] Verificar que o `DuelState` retornado contem as maos preenchidas

**DTOs envolvidos:** `DuelState`, `Player`

**Cenario de teste:**
```
deck com 40 cartas → apos setup: hand=5, deck=35
deck com 60 cartas → apos setup: hand=5, deck=55
deck vazio → nao deve quebrar, hand vazia
```

**Criterio de aceitacao:** Ao criar um duelo, cada jogador comeca com 5 cartas na mao e o deck embaralhado.

**Depende de:** BUG-002, BUG-003, BUG-004 resolvidos.

**Estimativa:** M

---

#### `[ ]` GAME-002 — Enriquecer Card com dados completos de atk/def/level/type

**Descricao:** Ao carregar cartas do deck-service, o `DuelApplicationServiceImpl.loadDeckFromService()` so preenche `cardId` e `name`. O ocgcore precisa de `atk`, `def`, `level`, `type` para processar acoes (calcular dano, validar invocacoes, etc.). Esses dados estao disponiveis no card-service.

**Onde:** `DuelApplicationServiceImpl.loadDeckFromService()`

**Checklist:**
- [ ] Adicionar endpoint no `DeckFeignClient` para buscar dados de multiplas cartas do card-service:
  ```java
  @GetMapping("/api/cards/internal?ids={ids}")
  List<CardSummaryDTO> findCardsByIds(@PathVariable("ids") String ids);
  ```
- [ ] Apos carregar os cardIds do deck, fazer uma chamada batch ao card-service para obter dados completos
- [ ] Mapear `CardSummaryDTO` para `Card` (preenchendo atk, def, level, type)
- [ ] Tratar fallback caso card-service esteja indisponivel (log + usar dados parciais)
- [ ] Adicionar campo `type` (CardType) no retorno de `CardSummaryDTO` no card-service se necessario

**DTOs:**
```java
// CardSummaryDTO (card-service side)
public record CardSummaryDTO(Long cardId, String name, String type,
                             String imageUrl, String description,
                             Integer atk, Integer def, Integer level) {}
```

**Criterio de aceitacao:** Cartas no `DuelState` possuem `atk`/`def`/`level`/`type` preenchidos apos criacao do duelo.

**Depende de:** BUG-003 (OpenFeign funcionando)

**Estimativa:** M

---

#### `[ ]` GAME-003 — Validar deck antes de criar duelo

**Descricao:** O deck carregado do deck-service deve ser validado contra as regras do Yu-Gi-Oh! (40-60 cartas no Main Deck, max 15 no Extra Deck, max 3 copias da mesma carta). Se o deck for invalido, o duelo deve ser recusado com erro claro.

**Onde:** `DuelApplicationServiceImpl.createDuel()`

**Checklist:**
- [ ] Antes de criar o `DuelState`, validar ambos os decks
- [ ] Regras de validacao:
  - Main Deck: 40 a 60 cartas
  - Extra Deck: 0 a 15 cartas (se existir)
  - Maximo de 3 copias de cada carta por deck (considerando main + extra + side)
- [ ] Se qualquer deck for invalido, lancar `InvalidDeckException` com detalhes das violacoes
- [ ] Registrar o erro no `GlobalExceptionHandler` com HTTP 400
- [ ] Retornar mensagem amigavel: `{"error": "Deck validation failed", "violations": ["Main deck must have 40-60 cards (current: 25)", "Card 'Dark Magician' has 4 copies (max: 3)"]}`

**Criterio de aceitacao:** Criacao de duelo com deck invalido retorna 400 com mensagem clara. Deck valido cria duelo normalmente.

**Estimativa:** M

---

#### `[ ]` GAME-004 — Publicar evento Kafka duel.encerrado

**Descricao:** Quando um duelo termina (game over, WO, desistencia), o duel-service deve publicar um evento no topico Kafka `duel.encerrado` para que o community-service possa atualizar o `duelStatus` dos jogadores de `IN_DUEL` para `AVAILABLE`.

**Onde:** `DuelApplicationServiceImpl.endDuel()`, `SessionHandler.handleDisconnectTimeoutAsync()`

**Checklist:**
- [ ] Adicionar dependencia `spring-kafka` no `build.gradle`
- [ ] Configurar Kafka producer no `application.yml`:
  ```yaml
  spring:
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  ```
- [ ] Criar classe `DuelEventKafkaPublisher` que implementa `DuelEventPublisherPort` (ou um publisher separado apenas para Kafka)
- [ ] Definir DTO do evento:
  ```java
  public record DuelEncerradoEvent(
      String duelId,
      String winnerId,
      String loserId,
      String playerAId,
      String playerBId,
      Integer turnCount,
      LocalDateTime finishedAt
  ) {}
  ```
- [ ] Publicar evento em TODOS os pontos de fim de duelo:
  - `DuelApplicationServiceImpl.endDuel()`
  - `SessionHandler.handleDisconnectTimeoutAsync()`
- [ ] Profile: habilitar Kafka apenas em perfis `!dev` (ou criar profile separado `kafka`)

**Topicos:**
| Topico | Chave | Valor |
|--------|-------|-------|
| `duel.encerrado` | `duelId` (String) | `DuelEncerradoEvent` (JSON) |

**Criterio de aceitacao:** Ao finalizar um duelo (normal ou por WO), um evento JSON e publicado no topico `duel.encerrado`.

**Depende de:** BUG-003 (OpenFeign + dependencias)

**Estimativa:** M

---

#### `[ ]` GAME-005 — Notificar community-service ao criar duelo

**Descricao:** Ao criar um duelo com sucesso, o duel-service deve notificar o community-service para que ambos os jogadores tenham seu `duelStatus` alterado para `IN_DUEL`. Pode ser via Kafka (topico `duel.iniciado`) ou via Feign direto no community-service.

**Checklist:**
- [ ] Opcao A (Kafka): Publicar evento `duel.iniciado` com `{ duelId, playerAId, playerBId }`
- [ ] Opcao B (Feign): Criar `CommunityFeignClient` que chama `PATCH /players/me/status` no community-service para cada jogador
- [ ] Executar a notificacao apos `repository.save(state)` em `createDuel()`
- [ ] Tratar falha da notificacao como warning (nao deve impedir a criacao do duelo)

**Criterio de aceitacao:** Apos criar um duelo, ambos os jogadores aparecem como `IN_DUEL` no community-service.

**Depende de:** GAME-004 (se for por Kafka)

**Estimativa:** M

---

#### `[ ]` GAME-006 — Popular duelType no historico

**Descricao:** `DuelHistoryMapper.toEntity()` nunca popula o campo `duelType`. Deve ser populado com informacao do tipo de duelo (ex: "RANKED", "CASUAL", "FRIENDLY").

**Onde:** `DuelHistoryMapper.toEntity()`, `CreateDuelRequest`

**Checklist:**
- [ ] Adicionar campo `duelType` (String) no `DuelState` com valor default `"CASUAL"`
- [ ] Mapear em `DuelHistoryMapper.toEntity()`: `duelType(state.getDuelType())`

**Criterio de aceitacao:** `duelType` no `DuelHistoryEntity` nunca e null apos um duelo terminar.

**Estimativa:** XS

---

### FASE 2 — Ciclo Completo (duelo jogavel ponta-a-ponta)

---

#### `[ ]` INT-001 — Redis usar TTL do application.yml

**Descricao:** `RedisDuelRepository` usa `TTL_HOURS = 24` hardcoded. Deve usar o valor configurado em `duel.redis.ttl-hours`.

**Onde:** `RedisDuelRepository`

**Checklist:**
- [ ] Injeter `@Value("${duel.redis.ttl-hours:24}")` no lugar da constante
- [ ] Usar o valor injetado nos metodos `save()` e `extendTtl()`

**Criterio de aceitacao:** Alterar `duel.redis.ttl-hours` no `application.yml` reflete no TTL do Redis.

**Estimativa:** XS

---

#### `[ ]` INT-002 — @Valid nos handlers WebSocket

**Descricao:** `DuelActionHandler` recebe `@Payload DuelActionDTO` e `@Payload PhaseChangeDTO` sem `@Valid`, entao as anotacoes `@NotBlank` nos DTOs sao ignoradas.

**Onde:** `DuelActionHandler`

**Checklist:**
- [ ] Adicionar `@Valid` antes de `@Payload`: `public void handleAction(@Valid @Payload DuelActionDTO action, ...)`
- [ ] Configurar tratamento de `MethodArgumentNotValidException` no `GlobalExceptionHandler`

**Criterio de aceitacao:** Enviar `DuelActionDTO` com `actionType` vazio pelo WebSocket retorna erro de validacao.

**Estimativa:** XS

---

#### `[ ]` INT-003 — Docker-compose para dev local

**Descricao:** Atualmente nao ha docker-compose para o duel-service. Para desenvolvimento local que precisa de Redis, e necessario um `docker-compose.yml` na raiz.

**Checklist:**
- [ ] Criar `docker-compose.yml` na raiz:
  ```yaml
  version: '3.8'
  services:
    redis:
      image: redis:7-alpine
      ports:
        - "6379:6379"
  ```
- [ ] Opcional: adicionar perfil `docker` que usa Redis + PostgreSQL

**Criterio de aceitacao:** `docker compose up -d` sobe Redis na porta 6379.

**Estimativa:** S

---

#### `[ ]` INT-004 — Dockerfile

**Descricao:** Criar Dockerfile para deploy conteinerizado do duel-service.

**Checklist:**
- [ ] Dockerfile multi-stage:
  ```dockerfile
  FROM gradle:8-jdk21 AS build
  WORKDIR /app
  COPY . .
  RUN ./gradlew bootJar

  FROM eclipse-temurin:21-jre
  WORKDIR /app
  COPY --from=build /app/build/libs/*.jar app.jar
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- [ ] Adicionar `.dockerignore` (node_modules, .git, build, .gradle)

**Criterio de aceitacao:** `docker build -t duel-service .` gera imagem funcional.

**Estimativa:** M

---

### FASE 3 — Qualidade, Testes e Documentacao

---

#### `[ ]` QLT-001 — Testes do ActionServiceImpl

**Onde:** `ActionServiceImpl`
**Checklist:**
- [ ] Mockar `OcgCorePort` e `DuelRepositoryPort`
- [ ] Testar `process()` com acao valida
- [ ] Testar `process()` com acao invalida → excecao
- [ ] Testar `process()` com duelo inexistente → excecao
- [ ] Testar `summon()`, `attack()`, `activateSpell()`

**Estimativa:** S

---

#### `[ ]` QLT-002 — Testes do PhaseServiceImpl

**Onde:** `PhaseServiceImpl`
**Checklist:**
- [ ] Testar `advance()` com cada fase
- [ ] Testar `isActionAllowed()` para cada combinacao fase/acao

**Estimativa:** S

---

#### `[ ]` QLT-003 — Testes de integracao Redis

**Onde:** `RedisDuelRepository`
**Checklist:**
- [ ] Usar Testcontainers com Redis
- [ ] Testar save + findById
- [ ] Testar delete
- [ ] Testar TTL expiry

**Estimativa:** M

---

#### `[ ]` QLT-004 — Testes de integracao WebSocket

**Onde:** `DuelActionHandler`, `WebSocketConfig`
**Checklist:**
- [ ] Usar `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` com `StompClient`
- [ ] Testar conexao STOMP com token JWT valido
- [ ] Testar conexao STOMP sem token → rejeitada
- [ ] Testar envio de acao via `/app/duel.action`
- [ ] Testar recebimento de estado em `/topic/duel/{id}`

**Estimativa:** M

---

#### `[ ]` QLT-005 — Testes do DuelEventPublisher

**Onde:** `DuelEventPublisher`
**Checklist:**
- [ ] Mockar `SimpMessagingTemplate`
- [ ] Testar `publishStateUpdate()`
- [ ] Testar `publishGameOver()`
- [ ] Testar `publishPlayerDisconnected()`
- [ ] Testar `publishPlayerReconnected()`

**Estimativa:** S

---

#### `[ ]` QLT-006 — CI/CD

**Checklist:**
- [ ] GitHub Actions workflow: `build.yml`
- [ ] Steps: checkout, setup JDK 21, `./gradlew build`, upload artifact

**Estimativa:** L

---

#### `[ ]` QLT-007 — Micrometer + Prometheus

**Checklist:**
- [ ] Adicionar `micrometer-registry-prometheus`
- [ ] Expor `/actuator/prometheus`
- [ ] Metricas: duelos criados, acoes processadas, erros, latencia WebSocket

**Estimativa:** M

---

#### `[ ]` QLT-008 — Testes do OcgCoreAdapter

**Onde:** `OcgCoreAdapter`
**Checklist:**
- [ ] Mockar `OcgCoreBridge`
- [ ] Testar `processAction()` com JSON valido
- [ ] Testar `processAction()` com JSON invalido → excecao tratada
- [ ] Testar `advancePhase()`
- [ ] Testar `isActionValid()` com retorno true/false

**Estimativa:** S

---

## Diagrama de Fluxo do Duelo

```
USUARIO                            FRONTEND                          DUEL-SERVICE
   │                                  │                                  │
   │  1. Fazer login                  │                                  │
   │─────────────────────────────────>│                                  │
   │                                  │──── POST /auth/login ───────────>│ (via auth-service)
   │                                  │<─── { jwt } ────────────────────│
   │<─── armazena token ─────────────│                                  │
   │                                  │                                  │
   │  2. Selecionar deck              │                                  │
   │─────────────────────────────────>│                                  │
   │                                  │──── GET /decks ─────────────────>│ (via deck-service)
   │                                  │<─── List<Deck> ─────────────────│
   │<─── escolhe deck ───────────────│                                  │
   │                                  │                                  │
   │  3. Criar duelo (ou aceitar)     │                                  │
   │─────────────────────────────────>│                                  │
   │                                  │──── POST /api/duels ────────────>│ (ou via community-service)
   │                                  │    { playerAId, playerBId,      │
   │                                  │      playerADeckId,              │
   │                                  │      playerBDeckId }             │
   │                                  │                                  │── shuffle + draw hand
   │                                  │                                  │── save state (Redis/InMemory)
   │                                  │<─── DuelResponse ───────────────│
   │                                  │                                  │
   │  4. Conectar WebSocket           │                                  │
   │─────────────────────────────────>│                                  │
   │                                  │──── STOMP CONNECT /ws ──────────>│
   │                                  │    Header: Authorization: Bearer │
   │                                  │<─── CONNECTED ──────────────────│
   │                                  │                                  │
   │                                  │──── SUBSCRIBE /topic/duel/{id} ─>│
   │                                  │<─── estado inicial ─────────────│
   │                                  │                                  │
   │  5. Jogar                        │                                  │
   │─────────────────────────────────>│                                  │
   │                                  │──── SEND /app/duel.action ──────>│
   │                                  │    { duelId, actionType,         │
   │                                  │      cardId, targetId }          │
   │                                  │                                  │── OcgCore.processAction()
   │                                  │<─── /topic/duel/{id} ───────────│
   │                                  │    { estado atualizado }         │
   │<─── renderiza novo estado ──────│                                  │
   │                                  │                                  │
   │  6. Duelo termina                │                                  │
   │                                  │                                  │── publish duel.encerrado (Kafka)
   │                                  │<─── /topic/duel/{id}/over ──────│
   │                                  │    { winnerId }                  │
   │<─── tela de resultado ──────────│                                  │
```

## Concluidas

- `[x]` Criacao de duelos via REST API — 2025-01 — PR #1
- `[x]` WebSocket STOMP para comunicacao em tempo real — 2025-01 — PR #2
- `[x]` Integracao com ocgcore via JNI — 2025-02 — PR #3
- `[x]` Gerenciamento de fases do duelo — 2025-02 — PR #4
- `[x]` Sistema de acoes (SUMMON, ATTACK, SPELL, SET) — 2025-03 — PR #5
- `[x]` Autenticacao JWT em handshake WebSocket — 2026-04-28 — JwtChannelInterceptor, JwtProperties
- `[x]` Disconnect handling com timeout de 3min — 2026-04-28 — SessionHandler, SessionManager
- `[x]` Migracao para Redis (persistent duel state) — 2026-04-28 — RedisDuelRepository
- `[x]` Duel history and result persistence — 2026-04-28 — DuelHistoryEntity, DuelHistoryRepository
- `[x]` Deck integration com deck-service via Feign — 2026-04-28 — DeckFeignClient
- `[x]` Testes unitarios — 2026-04-28 — DuelHistoryMapperTest, SessionManagerTest, DuelControllerHistoryTest

---

## Bugs Conhecidos

| ID | Descricao | Severidade | Reportado em |
|----|-----------|------------|--------------|
| BUG-001 | Jogador pode executar acoes fora da fase permitida (feedback ao cliente nao e claro) | Alta | 2025-03-15 |
| BUG-002 | DuelEventPublisher.publishGameOver() duplicado | Critica | 2026-07-07 |
| BUG-003 | Coordenada Maven OpenFeign invalida | Critica | 2026-07-07 |
| BUG-004 | Conflito de beans DuelRepositoryPort entre Redis e InMemory | Critica | 2026-07-07 |
| BUG-005 | V1__init_schema.sql vazio | Critica | 2026-07-07 |
| BUG-006 | Endpoints REST /api/duels/** sem autenticacao JWT | Alta | 2026-07-07 |
| BUG-007 | deck-service.url no application.yml aponta para porta 8082 — deck-service real roda na 8081 | Media | 2026-07-07 |

---

## Notas & Decisoes Pendentes

- [x] Decidir estrategia de persistencia de estado: Usar Redis
- [x] Definir formato de storage para historico de duelos: PostgreSQL/H2 via JPA
- [x] Configurar autenticacao WebSocket com auth-service
- [ ] Decidir se ocgcore sera substituido por engine em Java puro ou mantido via JNI
- [ ] Decidir versao do spring-cloud BOM no build.gradle
- [ ] Corrigir `deck-service.url` no `application.yml` de `8082` para `8081` (ver BUG-007)

---

## Historico de Versoes

| Versao | Data | Principais entregas |
|--------|------|---------------------|
| `0.0.6-SNAPSHOT` | 2026-04-28 | Deck integration via Feign |
| `0.0.5-SNAPSHOT` | 2026-04-28 | Duel history and result persistence |
| `0.0.4-SNAPSHOT` | 2026-04-28 | Migracao para Redis (persistent state) |
| `0.0.3-SNAPSHOT` | 2026-04-28 | Disconnect handling com timeout de 3min |
| `0.0.2-SNAPSHOT` | 2026-04-28 | Autenticacao JWT em WebSocket handshake |
| `0.0.1-SNAPSHOT` | 2025-04-17 | MVP com funcionalidades core |
