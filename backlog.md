# Backlog — duel-service

> Registro vivo do progresso do projeto. Atualizado a cada mudanca de estado de uma funcionalidade.
> **Ultima atualizacao:** 2026-07-09 — Sessao de trabalho: sincronizacao de posicoes via field_data (NATIVE-009), desenho da mao pelo motor C++ (NATIVE-010)

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

## Pendentes

### FASE 0 — Criticos (impedem compilacao/execucao)

---

#### `[x]` NATIVE-004 — Bridge compila contra ygopro-core real com Lua 5.4 e nlohmann_json

**Descricao:** O CMakeLists.txt foi reescrito para baixar e compilar Lua 5.4 (estatica), ygopro-core (estatica) e nlohmann_json (header-only) via FetchContent. O bridge C++ (ocgcore_bridge.cpp) compila com sucesso contra a API real do ygopro-core (ocgapi.h), gerando `libocgcore.so` (~2,9 MB) em `src/main/resources/native/`.

**Checklist:**
- [x] CMakeLists.txt com FetchContent para lua54, ygopro-core, nlohmann_json
- [x] Lua 5.4 compilado como biblioteca estatica excluindo lua.c/luac.c/onelua.c/ltests.c
- [x] ygopro-core compilado como biblioteca estatica linkada contra lua54
- [x] Bridge shared library linkada contra ocgcore-core + lua54
- [x] lib gerada em src/main/resources/native/ (tamanho ~2,9 MB)
- [x] Bridge usa OCG_CreateDuel, OCG_DuelProcess, OCG_DuelGetMessage, OCG_DuelSetResponse, OCG_DuelQueryField da API real

**Criterio de aceitacao:** `./gradlew fullBuildNative` compila o bridge contra ygopro-core real com Lua 5.4 integrado.

**Estimativa:** M

---

#### `[x]` NATIVE-001 — C++ JNI bridge para o ocgcore real

**Descricao:** Criar a implementacao C++ do JNI bridge que conecta `OcgCoreBridge.java` ao motor ygopro-core real. Antes existia apenas o stub Java (`OcgCoreStub`) e as declaracoes `native` sem implementacao C++ correspondente.

**O que foi criado:**
- `native/ocgcore-bridge/include/ocgcore_bridge_api.h` — API C esperada do ygopro-core
- `native/ocgcore-bridge/src/ocgcore_bridge.cpp` — Implementacao JNI (conversao Java ↔ C, delega ao ygopro-core)
- `native/ocgcore-bridge/CMakeLists.txt` — Build CMake que baixa ygopro-core via FetchContent e linka estaticamente
- `native/ocgcore-bridge/README.md` — Instrucoes de build
- `src/main/resources/native/README.md` — Atualizado com novo fluxo

**Integracao no build:**
- `build.gradle`: Tarefas `configureNative`, `buildNative`, `copyNativeLib`, `fullBuildNative`
- `Dockerfile`: Copia a lib nativa e configura `LD_LIBRARY_PATH`

**Checklist:**
- [x] JNI header gerado de `OcgCoreBridge.java`
- [x] Implementacao C++ do bridge (JNI ↔ C)
- [x] CMakeLists.txt com FetchContent do ygopro-core
- [x] Integracao Gradle (buildNative task)
- [x] Dockerfile atualizado
- [x] Testes de fallback (`OcgCoreLoaderTest`)
- [x] Documentacao atualizada

**Dependencia externa:** O binario final (`libocgcore.so`) depende do repositorio `edo9300/ygopro-core` para ser compilado via CMake FetchContent.

**Criterio de aceitacao:** `./gradlew test` passa, `./gradlew fullBuildNative` compila o bridge (requer ygopro-core acessivel via git). O fallback stub continua funcionando sem a lib nativa.

**Estimativa:** L

---

#### `[x]` DS-001 — Criar docs/data-model.md

**Descricao:** O CLAUDE.md exige o arquivo `docs/data-model.md` com a documentacao do modelo de dados, mas ele nao existe.

**Checklist:**
- [x] Criar `docs/data-model.md` seguindo o template em `~/Documentos/repos/claude-config/data-model-template.md`
- [x] Documentar todas as entidades: `DuelState`, `Player`, `Card`, `Zone`, `DuelHistoryEntity`
- [x] Documentar todos os enums: `Phase`, `GameStatus`, `CardType`, `CardPosition`, `ZoneType`
- [x] Incluir diagrama de relacoes entre entidades

**Criterio de aceitacao:** Arquivo criado com todas as entidades e enums do codigo real documentados.

**Estimativa:** S

---

### FASE 1 — Gameplay (motor do duelo funcional para um jogo jogavel)

---

#### `[x]` GAME-001 — Setup inicial: embaralhar deck e distribuir mao inicial

**Descricao:** Implementado em `DuelApplicationServiceImpl.initializePlayer()` — `shuffleDeck()` + `drawCards(deck, 5)`. Cada jogador comeca com 5 cartas na mao e deck embaralhado.

**Arquivo:** `DuelApplicationServiceImpl.java`

**Criterio de aceitacao:** Ao criar um duelo, cada jogador comeca com 5 cartas na mao e o deck embaralhado.

**Estimativa:** M — Concluido

---

#### `[x]` GAME-002 — Enriquecer Card com dados completos de atk/def/level/type

**Descricao:** Implementado em `DuelApplicationServiceImpl.loadDeckFromService()` — usa `extractInt()` e `extractType()` para popular atk/def/level/type com fallback para dados mock. Card.java ja possui todos os campos.

**Arquivo:** `DuelApplicationServiceImpl.java:134-141`, `Card.java`

**Criterio de aceitacao:** Cartas no `DuelState` possuem `atk`/`def`/`level`/`type` preenchidos apos criacao do duelo.

**Estimativa:** M — Concluido

---

#### `[x]` GAME-003 — Validar deck antes de criar duelo

**Descricao:** O deck carregado do deck-service deve ser validado contra as regras do Yu-Gi-Oh! (40-60 cartas no Main Deck, max 15 no Extra Deck, max 3 copias da mesma carta). Se o deck for invalido, o duelo deve ser recusado com erro claro.

**Onde:** `DuelApplicationServiceImpl.createDuel()`

**Checklist:**
- [x] Antes de criar o `DuelState`, validar ambos os decks
- [x] Regras de validacao:
  - Main Deck: 40 a 60 cartas
  - Extra Deck: 0 a 15 cartas (se existir)
  - Maximo de 3 copias de cada carta por deck (considerando main + extra + side)
- [x] Se qualquer deck for invalido, lancar `InvalidDeckException` com detalhes das violacoes
- [x] Registrar o erro no `GlobalExceptionHandler` com HTTP 400
- [x] Retornar mensagem amigavel: `{"error": "Deck validation failed", "violations": ["Main deck must have 40-60 cards (current: 25)", "Card 'Dark Magician' has 4 copies (max: 3)"]}`

**Criterio de aceitacao:** Criacao de duelo com deck invalido retorna 400 com mensagem clara. Deck valido cria duelo normalmente.

**Estimativa:** M

---

#### `[x]` GAME-004 — Publicar evento Kafka duel.encerrado

**Descricao:** Quando um duelo termina (game over, WO, desistencia), o duel-service deve publicar um evento no topico Kafka `duel.encerrado` para que o community-service possa atualizar o `duelStatus` dos jogadores de `IN_DUEL` para `AVAILABLE`.

**Onde:** `DuelApplicationServiceImpl.endDuel()`, `SessionHandler.handleDisconnectTimeoutAsync()`

**Checklist:**
- [x] Adicionar dependencia `spring-kafka` no `build.gradle`
- [x] Configurar Kafka producer no `application.yml`:
  ```yaml
  spring:
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  ```
- [x] Criar classe `DuelLifecycleKafkaPublisher` que publica `duel.iniciado` e `duel.encerrado`
- [x] Definir DTO do evento:
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
- [x] Publicar evento em TODOS os pontos de fim de duelo:
  - `DuelApplicationServiceImpl.endDuel()`
  - `SessionHandler.handleDisconnectTimeoutAsync()`
- [x] Profile: Kafka configurado por propriedade, com fallback local

**Topicos:**
| Topico | Chave | Valor |
|--------|-------|-------|
| `duel.encerrado` | `duelId` (String) | `DuelEncerradoEvent` (JSON) |

**Criterio de aceitacao:** Ao finalizar um duelo (normal ou por WO), um evento JSON e publicado no topico `duel.encerrado`.

**Depende de:** BUG-003 (OpenFeign + dependencias)

**Estimativa:** M

---

#### `[x]` GAME-005 — Notificar community-service ao criar duelo

**Descricao:** Ao criar um duelo com sucesso, o duel-service deve notificar o community-service para que ambos os jogadores tenham seu `duelStatus` alterado para `IN_DUEL`. Pode ser via Kafka (topico `duel.iniciado`) ou via Feign direto no community-service.

**Checklist:**
- [x] Opcao A (Kafka): Publicar evento `duel.iniciado` com `{ duelId, playerAId, playerBId }`
- [x] Opcao B (Feign): nao adotada, o fluxo ficou por Kafka no `duel.iniciado`
- [x] Executar a notificacao apos `repository.save(state)` em `createDuel()`
- [x] Tratar falha da notificacao como warning (nao deve impedir a criacao do duelo)

**Criterio de aceitacao:** Apos criar um duelo, ambos os jogadores aparecem como `IN_DUEL` no community-service.

**Depende de:** GAME-004 (se for por Kafka)

**Estimativa:** M

---

#### `[x]` GAME-006A — Primeiro turno: jogador inicial nao compra

**Descricao:** Implementado implicitamente — o jogo comeca em DRAW phase sem chamar `advancePhase()`, entao nenhuma compra ocorre. A flag `firstTurn` existe em DuelState e e setada para `false` no primeiro END→DRAW.

**Arquivo:** `OcgCoreStub.advancePhase():60-64`, `DuelState.java:25`

**Regra oficial:** `"O jogador que faz o primeiro turno não compra cards durante sua Fase de Compra."`

**Estimativa:** S — Concluido

---

#### `[x]` GAME-006B — Incrementar turno e alternar jogador ativo ao fim do ciclo

**Descricao:** Implementado em `OcgCoreStub.advancePhase()` — quando currentPhase == END, incrementa turnNumber, alterna activePlayerId, seta firstTurn=false e compra carta.

**Arquivo:** `OcgCoreStub.advancePhase():60-64`

**Estimativa:** S — Concluido

---

#### `[x]` GAME-006C — LP nunca negativo (floor em 0)

**Descricao:** Implementado em `Player.java` — `setLifePoints()` usa `Math.max(0, lifePoints)`, `takeDamage()` e `gainLife()` com validacao de valor negativo.

**Arquivo:** `Player.java:30-46`

**Estimativa:** XS — Concluido

---

#### `[x]` GAME-006D — Tratar empate (DRAW)

**Descricao:** Implementado em `OcgCoreStub.updateGameStatus()` — quando ambos os jogadores morrem, status=FINISHED e winnerId permanece null. `DuelHistoryMapper.toEntity()` ja trata winnerId==null como "DRAW".

**Arquivo:** `OcgCoreStub.java:273-285`, `DuelHistoryMapper.java:34`

**Estimativa:** S — Concluido

---

#### `[x]` GAME-006E — Side deck no modelo Player

**Descricao:** O domain model `Player` nao possui `sideDeck`. Apos o duelo, em formato match (melhor de 3), jogadores podem trocar cartas do side deck.

**Onde:** `Player.java`, `DuelApplicationServiceImpl.loadDeckFromService()`, `DeckFeignClient`

**Checklist:**
- [x] Adicionar `@Builder.Default private List<Card> sideDeck = new ArrayList<>();` no `Player`
- [x] No `DeckFeignClient`, carregar todas as zonas
- [x] No `loadDeckFromService()`, carregar tambem as cartas do side deck
- [x] Validar side deck (max 15 cartas)
- [x] Atualizar `DuelResponse` para incluir tamanho do side deck de cada jogador (informacional)

**Estimativa:** M

---

#### `[x]` GAME-006F — Zona de banimento no Player

**Descricao:** O domain model `Player` nao tem zona de banimento. Cartas banidas por efeitos nao tem onde ficar.

**Onde:** `Player.java`

**Checklist:**
- [x] Adicionar `@Builder.Default private List<Card> banished = new ArrayList<>();` no `Player`
- [x] Incluir no estado do duelo para ser renderizado no frontend

**Estimativa:** XS

---

#### `[x]` GAME-006G — Extra deck no Player

**Descricao:** O domain model `Player` nao tem `extraDeck`. Monstros do Extra Deck (Fusao, Sincronia, XYZ, Link) nao sao carregados.

**Onde:** `Player.java`, `DeckFeignClient`

**Checklist:**
- [x] Adicionar `@Builder.Default private List<Card> extraDeck = new ArrayList<>();` no `Player`
- [x] Carregar cartas do Extra Deck via `DeckFeignClient`
- [x] Validar extra deck (max 15 cartas)
- [x] Incluir no estado do duelo

**Estimativa:** S

---

#### `[x]` GAME-006 — Popular duelType no historico

**Descricao:** `DuelHistoryMapper.toEntity()` nunca popula o campo `duelType`. Deve ser populado com informacao do tipo de duelo (ex: "RANKED", "CASUAL", "FRIENDLY").

**Onde:** `DuelHistoryMapper.toEntity()`, `CreateDuelRequest`

**Checklist:**
- [x] Adicionar campo `duelType` (String) no `DuelState` com valor default `"CASUAL"`
- [x] Mapear em `DuelHistoryMapper.toEntity()`: `duelType(state.getDuelType())`

**Criterio de aceitacao:** `duelType` no `DuelHistoryEntity` nunca e null apos um duelo terminar.

**Estimativa:** XS

---

### FASE 2 — Ciclo Completo (duelo jogavel ponta-a-ponta)

---

#### `[x]` INT-001 — Redis usar TTL do application.yml

**Descricao:** `RedisDuelRepository` usa `TTL_HOURS = 24` hardcoded. Deve usar o valor configurado em `duel.redis.ttl-hours`.

**Onde:** `RedisDuelRepository`

**Checklist:**
- [x] Injeter `@Value("${duel.redis.ttl-hours:24}")` no lugar da constante
- [x] Usar o valor injetado nos metodos `save()` e `extendTtl()`

**Criterio de aceitacao:** Alterar `duel.redis.ttl-hours` no `application.yml` reflete no TTL do Redis.

**Estimativa:** XS

---

#### `[x]` INT-002 — @Valid nos handlers WebSocket

**Descricao:** `DuelActionHandler` recebe `@Payload DuelActionDTO` e `@Payload PhaseChangeDTO` sem `@Valid`, entao as anotacoes `@NotBlank` nos DTOs sao ignoradas.

**Onde:** `DuelActionHandler`

**Checklist:**
- [x] Adicionar `@Valid` antes de `@Payload`: `public void handleAction(@Valid @Payload DuelActionDTO action, ...)`
- [x] Configurar tratamento de `MethodArgumentNotValidException` no `GlobalExceptionHandler`

**Criterio de aceitacao:** Enviar `DuelActionDTO` com `actionType` vazio pelo WebSocket retorna erro de validacao.

**Estimativa:** XS

---

#### `[x]` INT-003 — Docker-compose para dev local

**Descricao:** Atualmente nao ha docker-compose para o duel-service. Para desenvolvimento local que precisa de Redis, e necessario um `docker-compose.yml` na raiz.

**Checklist:**
- [x] Criar `docker-compose.yml` na raiz:
  ```yaml
  version: '3.8'
  services:
    redis:
      image: redis:7-alpine
      ports:
        - "6379:6379"
  ```
- [x] Opcional: adicionar perfil `docker` que usa Redis + PostgreSQL

**Criterio de aceitacao:** `docker compose up -d` sobe Redis na porta 6379.

**Estimativa:** S

---

#### `[x]` INT-004 — Dockerfile

**Descricao:** Criar Dockerfile para deploy conteinerizado do duel-service.

**Checklist:**
- [x] Dockerfile multi-stage:
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
- [x] Adicionar `.dockerignore` (node_modules, .git, build, .gradle)

**Criterio de aceitacao:** `docker build -t duel-service .` gera imagem funcional.

**Estimativa:** M

---

### FASE 3A — Seguranca e Confiabilidade

---

#### `[x]` SEC-001 — Validar dono da acao no WebSocket

**Descricao:** O WebSocket nao verifica se o `playerId` na acao corresponde ao usuario autenticado. Um jogador malicioso pode enviar acoes como se fosse o oponente.

**Onde:** `DuelActionHandler.handleAction()`, `ActionServiceImpl.process()`

**Checklist:**
- [x] Em `handleAction()`, comparar `principal.getName()` com o `playerId` esperado
- [x] So permitir acao se o `activePlayerId` do estado for igual ao `principal.getName()`
- [x] Extra: verificar se o `playerId` da acao (actionDTO) corresponde ao `principal.getName()` do token
- [x] Retornar erro padrao: `"Nao e seu turno"` ou `"Acao nao autorizada"`

**Criterio de aceitacao:** Tentativa de agir como outro jogador ou fora do turno retorna erro 403.

**Estimativa:** M

---

#### `[x]` SEC-002 — Rate limiting nas acoes

**Descricao:** Sem limite de requisicoes, um jogador pode enviar centenas de acoes por segundo, sobrecarregando o servidor e o ocgcore.

**Checklist:**
- [x] Implementar rate limiter por `playerId` no `DuelActionHandler`:
  - Maximo de 10 acoes por segundo por jogador
  - Usar `TokenBucket` ou `RateLimiter` do Guava (ou implementacao simples com `ConcurrentHashMap` + timestamps)
  - Se excedido, retornar erro `"Muitas acoes. Aguarde."` e ignorar a acao
- [x] Configurar limite via `application.yml`: `duel.rate-limit.actions-per-second: 10`

**Estimativa:** S

---

#### `[x]` SEC-003 — Configurar CORS no duel-service

**Descricao:** Sem CORS configurado, o frontend em `localhost:5173` (Vite) nao consegue chamar os endpoints REST.

**Onde:** `SecurityConfig` ou `WebSocketConfig`

**Checklist:**
- [x] Adicionar config CORS global no `SecurityConfig` ou novo `@Bean WebMvcConfigurer`:
  ```java
  @Bean
  public WebMvcConfigurer corsConfigurer() {
      return new WebMvcConfigurer() {
          @Override
          public void addCorsMappings(CorsRegistry registry) {
              registry.addMapping("/**")
                      .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                      .allowedHeaders("*")
                      .allowCredentials(true);
          }
      };
  }
  ```

**Criterio de aceitacao:** Frontend Vite consegue chamar `POST /api/duels` sem erro CORS.

**Estimativa:** S

---

#### `[x]` SEC-004 — Redis: fallback se conexao falhar

**Descricao:** Se o Redis estiver indisponivel, `RedisDuelRepository` lanca excecao sem fallback. Deveria tentar InMemory como fallback.

**Onde:** `RedisDuelRepository`

**Checklist:**
- [x] Envolver operacoes `save()` / `findById()` em try-catch
- [x] Se Redis falhar, logar warning e usar `ConcurrentHashMap` local como fallback
- [x] Opcao 2 (mais limpa): injetar `DuelRepositoryPort` condicional via `@Profile("redis")` e `@Profile("!redis")`

**Estimativa:** M

---

#### `[x]` SEC-005 — Endpoint de resync de estado

**Descricao:** Se a conexao WebSocket cair e reconectar, o cliente precisa receber o estado atual completo. Atualmente so recebe o estado ao subscrever, mas se perder a mensagem, fica sem sincronia.

**Onde:** Novo endpoint REST ou mensagem STOMP dedicada

**Checklist:**
- [x] Criar endpoint `GET /api/duels/{duelId}/state` que retorna `DuelState` completo
- [x] No frontend, ao reconectar, chamar este endpoint antes de subscrever
- [x] Opcional: enviar versao incremental (incrementar a cada alteracao) para detectar divergencia

**Estimativa:** M

---

### FASE 3B — Qualidade, Testes e Documentacao

---

#### `[x]` QLT-001 — Testes do ActionServiceImpl

**Onde:** `ActionServiceImpl`
**Checklist:**
- [x] Mockar `OcgCorePort` e `DuelRepositoryPort`
- [x] Testar `process()` com acao valida
- [x] Testar `process()` com acao invalida → excecao
- [x] Testar `process()` com duelo inexistente → excecao
- [x] Testar `summon()`, `attack()`, `activateSpell()`

**Estimativa:** S

---

#### `[x]` QLT-002 — Testes do PhaseServiceImpl

**Onde:** `PhaseServiceImpl`
**Checklist:**
- [x] Testar `advance()` com cada fase
- [x] Testar `isActionAllowed()` para cada combinacao fase/acao

**Estimativa:** S

---

#### `[x]` QLT-003 — Testes de integracao Redis

**Onde:** `RedisDuelRepository`
**Checklist:**
- [x] Usar Testcontainers com Redis
- [x] Testar save + findById
- [x] Testar delete
- [x] Testar TTL expiry

**Estimativa:** M

---

#### `[x]` QLT-004 — Testes de integracao WebSocket

**Onde:** `DuelActionHandler`, `WebSocketConfig`
**Checklist:**
- [x] Usar `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` com `StompClient`
- [x] Testar conexao STOMP com token JWT valido
- [x] Testar conexao STOMP sem token → rejeitada
- [x] Testar envio de acao via `/app/duel.action`
- [x] Testar recebimento de estado em `/topic/duel/{id}`

**Estimativa:** M

---

#### `[x]` QLT-005 — Testes do DuelEventPublisher

**Onde:** `DuelEventPublisher`
**Checklist:**
- [x] Mockar `SimpMessagingTemplate`
- [x] Testar `publishStateUpdate()`
- [x] Testar `publishGameOver()`
- [x] Testar `publishPlayerDisconnected()`
- [x] Testar `publishPlayerReconnected()`

**Estimativa:** S

---

#### `[x]` QLT-006 — CI/CD

**Checklist:**
- [x] GitHub Actions workflow: `build.yml`
- [x] Steps: checkout, setup JDK 21, `./gradlew test`

**Estimativa:** L

---

#### `[x]` QLT-007 — Micrometer + Prometheus

**Checklist:**
- [x] Adicionar `micrometer-registry-prometheus`
- [x] Expor `/actuator/prometheus`
- [x] Metricas: duelos criados, acoes processadas, erros, latencia WebSocket

**Estimativa:** M

---

#### `[x]` QLT-008 — Testes do OcgCoreAdapter

**Onde:** `OcgCoreAdapter`
**Checklist:**
- [x] Mockar `OcgCoreBridge`
- [x] Testar `processAction()` com JSON valido
- [x] Testar `processAction()` com JSON invalido → excecao tratada
- [x] Testar `advancePhase()`
- [x] Testar `isActionValid()` com retorno true/false

**Estimativa:** S

---

---

### FASE 3C — Infraestrutura e Observabilidade

---

#### `[x]` INFRA-005 — Health endpoint

**Checklist:**
- [x] Adicionar `spring-boot-starter-actuator`
- [x] Expor `/actuator/health` como unica excecao no `SecurityConfig`
- [x] Configurar liveness/readiness probes (importante para Kubernetes)

**Estimativa:** XS

---

#### `[x]` INFRA-006 — STOMP heartbeats

**Descricao:** Sem heartbeat configurado, conexoes WebSocket ociosas ficam abertas para sempre.

**Onde:** `WebSocketConfig`

**Checklist:**
- [x] No `configureMessageBroker()`, configurar heartbeat:
  ```java
  registry.enableSimpleBroker("/topic")
         .setHeartbeatValue(new long[]{10000, 10000}); // client, server (ms)
  ```
- [x] Heartbeat do servidor configurado (10s). O heartbeat do cliente (frontend) e responsabilidade do repositorio `yu-gi-oh-deck-management-front-end` (item WEB-022/.env + configuracao do STOMP Client).

**Estimativa:** XS

---

#### `[x]` INFRA-007 — Graceful shutdown

**Descricao:** Ao parar o servico, as conexoes WebSocket ativas sao dropadas sem aviso.

**Onde:** `application.yml` ou bean `SmartLifecycle`

**Checklist:**
- [x] Em `application.yml`: `server.shutdown: graceful`
- [x] Opcional: listener `ContextClosedEvent` que notifica jogadores em duelo ativo antes de desligar

**Estimativa:** S

---

#### `[x]` INFRA-008 — Correlation ID nos logs

**Descricao:** Sem um ID de correlacao, e impossivel rastrear uma requisicao do frontend ate o ocgcore.

**Checklist:**
- [x] Adicionar filtro MDC que injeta `X-Correlation-Id` do header HTTP ou gera um UUID
- [x] Configurar `logging.pattern.level` para incluir `%X{correlationId}`
- [x] Propagar o correlationId para chamadas Feign via `RequestInterceptor`

**Estimativa:** S

---

#### `[x]` INFRA-009 — OcgCore nativo com fallback para Stub

**Descricao:** Em producao, se o ocgcore nativo falhar ao carregar, o servico quebra. Deveria cair para o `OcgCoreStub` como fallback.

**Onde:** `OcgCoreLoader`, `OcgCoreConfig`

**Checklist:**
- [x] Se `System.load()` falhar no `OcgCoreLoader`, logar erro e usar `OcgCoreStub`
- [x] Opcao: criar `@ConditionalOnClass` ou `@ConditionalOnMissingBean` para resolver

**Estimativa:** S

---

### FASE 3D — Dados e Persistencia

---

#### `[x]` DATA-001 — Salvar deckIds no historico

**Descricao:** `DuelHistoryEntity` nao armazena os `deckId`s usados no duelo. Isso impede de reconstruir o contexto do duelo depois.

**Onde:** `DuelHistoryEntity`, `DuelHistoryMapper`

**Checklist:**
- [x] Adicionar colunas `player_a_deck_id` e `player_b_deck_id` (Long)
- [x] No `DuelHistoryMapper.toEntity()`, preencher os campos
- [x] Atualizar `V1__init_schema.sql`

**Estimativa:** XS

---

#### `[x]` DATA-002 — Salvar tipo de vitoria (normal/WO/disconnect)

**Descricao:** O historico nao diferencia entre vitoria normal, WO por desconexao, ou conceder.

**Onde:** `DuelHistoryEntity`, `DuelApplicationServiceImpl`, `SessionHandler`

**Checklist:**
- [x] Adicionar coluna `victory_type` (String): `"NORMAL"`, `"WO"`, `"SURRENDER"`, `"DRAW"`
- [x] Em `endDuel()`, aceitar parametro `winType`
- [x] Em `SessionHandler.handleDisconnectTimeoutAsync()`, passar `"WO"`

**Estimativa:** XS

---

#### `[x]` DATA-003 — Incluir imageUrl das cartas no estado do duelo

**Descricao:** O frontend precisa da URL da imagem de cada carta. Sem ela, o frontend precisa fazer N chamadas ao card-service.

**Onde:** `Card.java`, `DuelApplicationServiceImpl.loadDeckFromService()`

**Checklist:**
- [x] Adicionar `private String imageUrl;` no `Card.java`
- [x] Ao carregar cartas do deck-service, buscar `imageUrl` do card-service via Feign batch
- [x] Incluir `imageUrl` no `DuelState` que e enviado ao frontend via WebSocket

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

## Pendentes

### FASE 3E — Engine Nativa (bridge real)

---

#### `[x]` NATIVE-009 — Bridge stateful: sincronizar posicoes das cartas entre Java e C++

**Descricao:** O bridge C++ retorna `field_data` via `OCG_DuelQueryField` contendo as posicoes atuais de todas as cartas. O `OcgCoreAdapter.applyEngineResult()` agora extrai esse campo e sincroniza as listas do `DuelState` Java (monsterZones, spellTrapZones, deck, hand, graveyard, banished) com os dados retornados pelo motor.

**Checklist:**
- [x] Extrair do `engine.field` as posicoes de cada carta por jogador
- [x] Sincronizar MonsterZones e SpellTrapZones via `syncZoneList()`
- [x] Atualizar contagens de deck/hand/grave/banished via `trimList()`
- [x] Mapear posicoes do C++ (POS_FACEUP_ATTACK, POS_FACEDOWN_DEFENSE, etc.) para `CardPosition` Java

**Implementacao:** `OcgCoreAdapter.java:109-178` — metodos `syncFieldPositions()`, `syncZoneList()`, `mapCardPosition()`, `trimList()`

**Estimativa:** M

---

#### `[x]` NATIVE-010 — Desenho da mao inicial pelo motor C++

**Descricao:** O bridge C++ agora usa `startingDrawCount=5` em vez de `startingDrawCount=0`. O Java (`DuelApplicationServiceImpl.initializePlayer()`) nao distribui mais as 5 cartas iniciais — isso e feito pelo `OCG_StartDuel()` no C++. O primeiro turno continua sem compra (regra oficial mantida pelo motor C++).

**Checklist:**
- [x] Bridge C++: `startingDrawCount` alterado de 0 para 5
- [x] Java: `INITIAL_HAND_SIZE` alterado para 0
- [x] Java: removida a chamada `drawCards(deck, INITIAL_HAND_SIZE)`
- [x] Java: `hand` inicializado como lista vazia em vez de receber as cartas

**Estimativa:** XS

---

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
- `[x]` BUG-002: DuelEventPublisher.publishGameOver() duplicado — 2026-07-07 — Corrigido
- `[x]` BUG-003: Coordenada Maven OpenFeign invalida — 2026-07-07 — Corrigido
- `[x]` BUG-004: Conflito de beans DuelRepositoryPort — 2026-07-07 — Corrigido
- `[x]` BUG-005: V1__init_schema.sql vazio — 2026-07-07 — Corrigido
- `[x]` BUG-006: Endpoints REST sem autenticacao JWT — 2026-07-07 — Corrigido
- `[x]` BUG-007: deck-service.url porta 8082 → 8081 — 2026-07-07 — Corrigido
- `[x]` BUG-010: LP floor em 0 (takeDamage) — 2026-07-07 — Corrigido
- `[x]` GAME-001: Shuffle + distribuir mao inicial — 2026-07-07 — Implementado
- `[x]` GAME-002: Enriquecer Card com atk/def/level/type — 2026-07-07 — Implementado
- `[x]` GAME-006A: Primeiro turno sem compra — 2026-07-07 — Implementado (implicito)
- `[x]` GAME-006B: Incrementar turno + alternar jogador — 2026-07-07 — Implementado
- `[x]` GAME-006C: LP nunca negativo (floor em 0) — 2026-07-07 — Implementado
- `[x]` GAME-006D: Tratar empate (DRAW) — 2026-07-07 — Implementado
- `[x]` DS-001: Criar docs/data-model.md — 2026-07-07 — Implementado
- `[x]` NATIVE-001: C++ JNI bridge para ocgcore real — 2026-07-09 — Implementado
- `[x]` NATIVE-002: Testes do OcgCoreLoader/fallback — 2026-07-09 — Implementado
- `[x]` NATIVE-003: Dockerfile com suporte a lib nativa — 2026-07-09 — Implementado
- `[x]` NATIVE-004: Bridge compila contra ygopro-core real com Lua 5.4 e nlohmann_json — 2026-07-09 — Implementado
- `[x]` NATIVE-005: OcgCoreBridgeResponse + applyEngineResult() — 2026-07-09 — Implementado
- `[x]` NATIVE-006: Bridge stateful (OCG_Duel persistente por duelId) — 2026-07-09 — Implementado
- `[x]` NATIVE-007: Raw WebSocket endpoint `/ws-raw` para clientes sem SockJS — 2026-07-09 — Implementado
- `[x]` NATIVE-008: Constantes de fase do ygopro-core atualizadas (0x01...0x200) — 2026-07-09 — Implementado
- `[x]` BUG-011: `engine.field` string mal-parseada — bridge retornava objeto JSON, Java esperava String — Corrigido

---

## Bugs Conhecidos

| ID | Descricao | Severidade | Reportado em |
|----|-----------|------------|--------------|
| BUG-001 | Jogador pode executar acoes fora da fase permitida (feedback ao cliente nao e claro) | Alta | 2025-03-15 |
| ~~BUG-002~~ | ~~DuelEventPublisher.publishGameOver() duplicado~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| ~~BUG-003~~ | ~~Coordenada Maven OpenFeign invalida~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| ~~BUG-004~~ | ~~Conflito de beans DuelRepositoryPort entre Redis e InMemory~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| ~~BUG-005~~ | ~~V1__init_schema.sql vazio~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| ~~BUG-006~~ | ~~Endpoints REST /api/duels/** sem autenticacao JWT~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| ~~BUG-007~~ | ~~deck-service.url no application.yml aponta para porta 8082 — deck-service real roda na 8081~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |
| BUG-008 | Card.java nao tem imageUrl — frontend nao consegue exibir arte da carta sem buscar separadamente | Media | 2026-07-07 |
| BUG-009 | StompPrincipal nao implementa equals/hashCode — pode causar problemas em collections do Spring Security | Baixa | 2026-07-07 |
| ~~BUG-010~~ | ~~LP pode ficar negativo — Player.lifePoints nao tem protecao contra valores abaixo de 0~~ — Corrigido em 2026-07-07 | Resolvido | 2026-07-07 |

---

## Notas & Decisoes Pendentes

- [x] Decidir estrategia de persistencia de estado: Usar Redis
- [x] Definir formato de storage para historico de duelos: PostgreSQL/H2 via JPA
- [x] Configurar autenticacao WebSocket com auth-service
- [x] Decidir se ocgcore sera substituido por engine em Java puro ou mantido via JNI: mantido via JNI
- [x] Decidir versao do spring-cloud BOM no build.gradle: `2023.0.0`
- [x] Corrigir `deck-service.url` no `application.yml` de `8082` para `8081` (ver BUG-007 — corrigido em 2026-07-07)
- [x] Definir se side deck sera suportado na primeira versao ou apenas em matches (melhor de 3): suportado em matches
- [x] Definir taxa de rate limiting (10 acoes/s parece razoavel): `10 acoes/s`
- [x] Decidir sobre heartbeat WebSocket: necessario para evitar acumulo de conexoes ociosas

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
