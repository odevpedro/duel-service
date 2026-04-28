# Backlog — duel-service

> Registrovivo do progresso do projeto. Atualizado a cada mudança de estado de uma funcionalidade.
> **Última atualização:** 2026-04-28

---

## Sobre o Projeto

API REST e WebSocket para gerenciamento de duelos Yu-Gi-Oh! em tempo real, integrando o frontend React com o motor C++ ocgcore via JNI.

**Versão atual:** `0.0.1-SNAPSHOT`
**Repositório:** [github.com/odevpedro/duel-service](https://github.com/odevpedro/duel-service)
**Stack principal:** Java 21 + Spring Boot 3.2 + WebSocket/STOMP + JNI + ocgcore

---

## Legenda

| Símbolo | Significado |
|---------|-------------|
| `[ ]`   | Pendente |
| `[~]`   | Em andamento |
| `[x]`   | Concluído |
| `P0`    | Crítico — bloqueia outras features |
| `P1`    | Alta prioridade |
| `P2`    | Média prioridade |
| `P3`    | Melhoria / nice-to-have |
| `XS` `S` `M` `L` `XL` | Estimativa de complexidade |

---

## Em Andamento

> Features atualmente sendo desenvolvidas. Idealmente, maximo de 2–3 itens simultaneos.

Nenhuma feature em andamento.

---

## Pendentes

> Ordenadas por prioridade. Itens de P0 e P1 devem entrar em "Em Andamento" primeiro.

Nenhuma feature pendente.

---

## Concluídas

> Features finalizadas com suas respectivas datas de conclusão e links de referência.

- `[x]` Criação de duelos via REST API — 2025-01 — PR #1
- `[x]` WebSocket STOMP para komunicação em tempo real — 2025-01 — PR #2
- `[x]` Integração com ocgcore via JNI — 2025-02 — PR #3
- `[x]` Gerenciamento de fases do duelo — 2025-02 — PR #4
- `[x]` Sistema de ações (SUMMON, ATTACK, SPELL, SET) — 2025-03 — PR #5
- `[x]` Autenticação JWT em handshake WebSocket — 2026-04-28 — JwtChannelInterceptor, JwtProperties
- `[x]` Disconnect handling com timeout de 3min — 2026-04-28 — SessionHandler, SessionManager
- `[x]` Migração para Redis (persistent duel state) — 2026-04-28 — RedisDuelRepository
- `[x]` Duel history and result persistence — 2026-04-28 — DuelHistoryEntity, DuelHistoryRepository
- `[x]` Deck integration com deck-service via Feign — 2026-04-28 — DeckFeignClient
- `[x]` Testes unitários — 2026-04-28 — DuelHistoryMapperTest, SessionManagerTest, DuelControllerHistoryTest

---

## Bugs Conhecidos

> Problemas identificados que ainda não foram corrigidos.

| ID | Descrição | Severidade | Reportado em |
|----|-----------|------------|--------------|
| BUG-001 | Jogador pode executar ações fora da fase permitida | Alta | 2025-03-15 |

> Nota: A validação está sendo feita pelo ocgcore, mas o feedback ao cliente não é claro. Necessário tratar exceção e retornar mensagem adequada.

---

## Notas & Decisões Pendentes

> Pontos em aberto que precisam de decisão antes de serem desenvolvidos.

- Decidir estratégia de persistência de estado: [x] Usar Redis
- Definir formato de storage para histórico de duelos: [x] PostgreSQL/H2 via JPA
- [x] CONFIGURAR authentication WebSocket com auth-service

---

## Histórico de Versões

| Versão | Data | Principais entregas |
|--------|------|---------------------|
| `0.0.6-SNAPSHOT` | 2026-04-28 | Deck integration via Feign |
| `0.0.5-SNAPSHOT` | 2026-04-28 | Duel history and result persistence |
| `0.0.4-SNAPSHOT` | 2026-04-28 | Migração para Redis (persistent state) |
| `0.0.3-SNAPSHOT` | 2026-04-28 | Disconnect handling com timeout de 3min |
| `0.0.2-SNAPSHOT` | 2026-04-28 | Autenticação JWT em WebSocket handshake |
| `0.0.1-SNAPSHOT` | 2025-04-17 | MVP com funcionalidades core |