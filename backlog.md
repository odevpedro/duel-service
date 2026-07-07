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
| `XS` `S` `M` `L` `XL` | Estimativa de complexidade |

---

## Em Andamento

> Features atualmente sendo desenvolvidas. Idealmente, maximo de 2–3 itens simultaneos.

Nenhuma feature em andamento.

---

## Pendentes

### FASE 0 — Criticos (impedem compilacao/execucao)

| ID | Feature | Prioridade | Estimativa |
|----|---------|------------|------------|
| BUG-002 | DuelEventPublisher.publishGameOver() duplicado — quebra compilacao | P0 | XS |
| BUG-003 | build.gradle linha 22 — coordenada 'org.springframework.cloud/openfeign' invalida (falta BOM) | P0 | XS |
| BUG-004 | RedisDuelRepository sem @Profile — conflito com InMemoryDuelRepository em perfil dev | P0 | S |
| BUG-005 | V1__init_schema.sql vazio — migration Flyway nao cria tabela duel_history | P0 | XS |
| BUG-006 | SecurityConfig libera /api/duels/** sem autenticacao — endpoints REST sem JWT | P0 | S |
| DS-001 | docs/data-model.md ausente — exigido pelo template CLAUDE.md | P0 | S |

### FASE 1 — Gameplay (motor do duelo funcional)

| ID | Feature | Prioridade | Estimativa |
|----|---------|------------|------------|
| GAME-001 | Integrar Kafka — publicar topico duel.encerrado ao fim do duelo (community-service consome) | P1 | M |
| GAME-002 | Setup inicial completo — createDuel() deve embaralhar deck e distribuir mao inicial | P1 | M |
| GAME-003 | Enriquecer Card com dados completos — buscar atk/def/level/type do card-service ou YGOPRODeck | P1 | M |
| GAME-004 | Popular duelType no DuelHistoryMapper.toEntity() | P1 | XS |
| GAME-005 | Validar deck antes do duelo — verificar regras (40-60 main, max 15 extra, max 3 copias) | P1 | M |
| GAME-006 | Publicar evento ao criar duelo — notificar community-service que jogadores entraram em IN_DUEL | P1 | M |

### FASE 2 — Ciclo Completo (integracao ponta-a-ponta)

| ID | Feature | Prioridade | Estimativa |
|----|---------|------------|------------|
| INT-001 | Redis usar TTL configuravel — substituir hardcoded 24h por duel.redis.ttl-hours do application.yml | P2 | XS |
| INT-002 | Adicionar @Valid nos @Payload do WebSocket (DuelActionHandler) | P2 | XS |
| INT-003 | Docker-compose para dev local com Redis | P2 | S |
| INT-004 | Dockerfile para deploy conteinerizado | P2 | M |

### FASE 3 — Qualidade & Infra

| ID | Feature | Prioridade | Estimativa |
|----|---------|------------|------------|
| QLT-001 | Testes de integracao Redis (RedisDuelRepository) | P2 | M |
| QLT-002 | Testes de integracao JPA (DuelHistoryRepository) | P2 | M |
| QLT-003 | Testes de integracao WebSocket STOMP | P2 | M |
| QLT-004 | Testes do ActionServiceImpl | P2 | S |
| QLT-005 | Testes do PhaseServiceImpl | P2 | S |
| QLT-006 | Testes do DuelEventPublisher | P2 | S |
| QLT-007 | Testes do OcgCoreAdapter | P3 | S |
| QLT-008 | CI/CD — pipeline de build, teste e deploy | P3 | L |
| QLT-009 | Monitoring — metrics com Micrometer + Prometheus | P3 | M |

---

## Concluidas

> Features finalizadas com suas respectivas datas de conclusao e links de referencia.

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

> Problemas identificados que ainda nao foram corrigidos.

| ID | Descricao | Severidade | Reportado em |
|----|-----------|------------|--------------|
| BUG-001 | Jogador pode executar acoes fora da fase permitida | Alta | 2025-03-15 |
| BUG-002 | DuelEventPublisher.publishGameOver() duplicado — quebra compilacao | Critica | 2026-07-07 |
| BUG-003 | build.gradle — coordenada org.springframework.cloud/openfeign invalida | Critica | 2026-07-07 |
| BUG-004 | Conflito de beans DuelRepositoryPort entre RedisDuelRepository e InMemoryDuelRepository | Critica | 2026-07-07 |
| BUG-005 | V1__init_schema.sql vazio | Critica | 2026-07-07 |
| BUG-006 | Endpoints REST /api/duels/** sem autenticacao JWT | Alta | 2026-07-07 |

> Nota BUG-001: A validacao esta sendo feita pelo ocgcore, mas o feedback ao cliente nao e claro. Necessario tratar excecao e retornar mensagem adequada.

---

## Notas & Decisoes Pendentes

> Pontos em aberto que precisam de decisao antes de serem desenvolvidos.

- [x] Decidir estrategia de persistencia de estado: Usar Redis
- [x] Definir formato de storage para historico de duelos: PostgreSQL/H2 via JPA
- [x] Configurar autenticacao WebSocket com auth-service
- [ ] Decidir se ocgcore sera substituido por engine em Java puro ou mantido via JNI
- [ ] Definir versao do spring-cloud BOM no build.gradle

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
