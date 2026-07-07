# Roadmap de Implementacao — Ecossistema Duelo

> Ordem recomendada para implementar tudo que esta nos backlogs.
> Cada fase entrega um marco jogavel.

---

## FASE 0 — Fundacao (servico compila + sobe)

**Objetivo:** duel-service compila, sobe e os servicos conversam entre si.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 1 | BUG-002: metodo duplicado publishGameOver | duel-service | — |
| 2 | BUG-003: coordenada Maven OpenFeign | duel-service | — |
| 3 | BUG-004: @Profile no RedisDuelRepository | duel-service | — |
| 4 | BUG-005: popular V1__init_schema.sql | duel-service | — |
| 5 | BUG-006: autenticacao JWT nos REST endpoints | duel-service | — |
| 6 | BUG-007: corrigir deck-service.url 8082→8081 | duel-service | — |
| 7 | CONFIG-001: CORS em todos os servicos | monorepo (todos) | — |
| 8 | CONFIG-002: sincronizar portas entre servicos | monorepo | — |
| 9 | BUG-010: LP floor em 0 (takeDamage) | duel-service | — |

**Tempo estimado:** 2-3 dias

---

## FASE 1 — MVP Jogavel (1 jogador, duelo local via WebSocket)

**Objetivo:** Usuario faz login, cria um duelo, ve o campo, joga uma carta, ataca, e ve o fim do duelo.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 10 | GAME-001: shuffle + distribuir mao inicial | duel-service | Fase 0 |
| 11 | GAME-002: enriquecer cards com atk/def/level | duel-service | Fase 0 |
| 12 | GAME-006A: primeiro turno sem compra | duel-service | Fase 0 |
| 13 | GAME-006B: incrementar turno + alternar jogador | duel-service | Fase 0 |
| 14 | GAME-006D: tratar empate (DRAW) | duel-service | Fase 0 |
| 15 | DS-001: criar docs/data-model.md | duel-service | Fase 0 |
| 16 | WEB-000: npm install @stomp/stompjs sockjs-client | frontend | — |
| 17 | WEB-001+005: tela de login + JWT | frontend | — |
| 18 | WEB-003: remover arquivos duplicados | frontend | — |
| 19 | QLT-003: react-router-dom | frontend | WEB-001 |
| 20 | WEB-002: WebSocketEngine | frontend | WEB-000 |
| 21 | WEB-006: Lobby + criar duelo | frontend | WEB-001, WEB-002 |
| 22 | WEB-008: tela de resultado | frontend | WEB-006 |
| 23 | WEB-013: loading states | frontend | — |
| 24 | WEB-018: sistema de toast | frontend | — |
| 25 | GAME-007: compra automatica na DRAW phase | frontend | — |
| 26 | GAME-004: limite dinamico de deck | frontend | — |
| 27 | INT-002: @Valid nos handlers WebSocket | duel-service | Fase 0 |

**Tempo estimado:** 1-2 semanas

---

## FASE 2 — Regras Completas (duelo justo e correto)

**Objetivo:** Regras oficiais do Yu-Gi-Oh! implementadas — sem cheat possivel.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 28 | SEC-001: validar dono da acao no WebSocket | duel-service | Fase 1 |
| 29 | SEC-002: rate limiting nas acoes | duel-service | Fase 1 |
| 30 | GAME-006F: zona de banimento (Player) | duel-service | Fase 1 |
| 31 | GAME-006G: extra deck (Player) | duel-service | Fase 1 |
| 32 | GAME-003: validar deck antes do duelo | duel-service | Fase 1 |
| 33 | GAME-005: popular duelType no historico | duel-service | Fase 1 |
| 34 | DATA-001: salvar deckIds no historico | duel-service | Fase 1 |
| 35 | DATA-002: salvar winType (WO/NORMAL) | duel-service | Fase 1 |
| 36 | DATA-003: imageUrl das cartas no estado | duel-service | Fase 1 |
| 37 | WEB-009: historico de duelos | frontend | Fase 1 |
| 38 | WEB-010: renderizar cartas reais da API | frontend | Fase 1 |
| 39 | GAME-002: validacao completa de regras | frontend | Fase 1 |
| 40 | GAME-003: posicao de defesa | frontend | Fase 1 |
| 41 | WEB-015: zona de banimento no campo | frontend | Fase 1 |
| 42 | WEB-016: zona de extra deck | frontend | Fase 1 |
| 43 | WEB-024: drag feedback + drop zone highlight | frontend | Fase 1 |

**Tempo estimado:** 1-2 semanas

---

## FASE 3 — Multiplayer Real (matchmaking + duelo online)

**Objetivo:** Dois jogadores se encontram, um desafia o outro, duelam e o resultado persiste.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 44 | INT-001: consumer Kafka duel.encerrado | monorepo community | Fase 2 |
| 45 | INT-002: ChallengeService.accept() criar duelo | monorepo community | Fase 2 |
| 46 | INT-003: add targetDeckId no accept | monorepo community | Fase 2 |
| 47 | GAME-004: publicar evento Kafka duel.encerrado | duel-service | Fase 2 |
| 48 | GAME-005: notificar community-service ao criar duelo | duel-service | Fase 2 |
| 49 | SEC-005: endpoint de resync de estado | duel-service | Fase 2 |
| 50 | WEB-006b: matchmaking (jogadores proximos) | frontend | Fase 2 |
| 51 | WEB-007: selecao de deck | frontend | Fase 2 |
| 52 | WEB-011: reconexao automatica WebSocket | frontend | Fase 2 |
| 53 | WEB-012: botao de conceder | frontend | Fase 2 |
| 54 | WEB-014: log de acoes do duelo | frontend | Fase 2 |
| 55 | WEB-023: chat entre jogadores | frontend + duel-service | Fase 2 |

**Tempo estimado:** 2-3 semanas

---

## FASE 4 — Qualidade e Confiabilidade

**Objetivo:** Sistema robusto, testado, com fallbacks e observavel.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 56 | SEC-003: CORS (ja deve estar) | duel-service | Fase 0 |
| 57 | SEC-004: Redis fallback para InMemory | duel-service | Fase 3 |
| 58 | INFRA-005: health endpoint | duel-service | Fase 3 |
| 59 | INFRA-006: STOMP heartbeats | duel-service | Fase 3 |
| 60 | INFRA-007: graceful shutdown | duel-service | Fase 3 |
| 61 | INFRA-008: correlation ID | duel-service | Fase 3 |
| 62 | INFRA-009: OcgCore fallback para Stub | duel-service | Fase 3 |
| 63 | QLT-001 a QLT-005: testes unitarios | duel-service | Fase 3 |
| 64 | QLT-008: teste OcgCoreAdapter | duel-service | Fase 3 |
| 65 | QLT-006: CI/CD | duel-service | Fase 3 |
| 66 | INT-003: docker-compose dev com Redis | duel-service | Fase 3 |
| 67 | INT-004: Dockerfile | duel-service | Fase 3 |
| 68 | GAME-006E: side deck | duel-service | Fase 3 |
| 69 | AUTH-004: rate limit no login | monorepo auth | Fase 3 |
| 70 | SEC-001: correlation ID entre servicos | monorepo shared | Fase 3 |
| 71 | SEC-002: circuit breaker deck→card | monorepo deck | Fase 3 |
| 72 | TEST-001 a TEST-004: testes monorepo | monorepo | Fase 3 |
| 73 | DOC-001: collection Postman | monorepo | Fase 3 |
| 74 | DOC-002: ADRs | monorepo | Fase 3 |

**Tempo estimado:** 2-3 semanas

---

## FASE 5 — Infinity (onboarding, motion, UX, polimento)

**Objetivo:** Experiencia do usuario refinada com animacoes, som, tutorial.

| Ordem | Task | Repo | Depende de |
|-------|------|------|------------|
| 75 | MOTION-001: framer-motion | frontend | Fase 3 |
| 76 | MOTION-002: onboarding tutorial (7 steps) | frontend | MOTION-001 |
| 77 | MOTION-003: transicoes entre paginas | frontend | MOTION-001 |
| 78 | MOTION-004: glow em cartas jogaveis | frontend | MOTION-001 |
| 79 | MOTION-005: screen shake em dano | frontend | Fase 3 |
| 80 | MOTION-006: particulas vitoria/derrota | frontend | WEB-008 |
| 81 | MOTION-007: flip 3D face-down | frontend | Fase 3 |
| 82 | MOTION-008: transicao de fases melhorada | frontend | Fase 3 |
| 83 | WEB-019: card detail modal | frontend | Fase 3 |
| 84 | WEB-020: timer de turno | frontend | Fase 3 |
| 85 | WEB-021: pagina 404 | frontend | Fase 3 |
| 86 | WEB-022: .env para URLs | frontend | Fase 3 |
| 87 | WEB-025: responsividade | frontend | Fase 3 |
| 88 | WEB-026: cache local de cartas | frontend | Fase 3 |
| 89 | WEB-027: gestao offline | frontend | Fase 3 |
| 90 | UX-001: efeitos sonoros | frontend | Fase 3 |
| 91 | QLT-001 a QLT-008: qualidade frontend | frontend | Fase 3 |
| 92 | INFRA-001: Elasticsearch | monorepo | Fase 4 |
| 93 | INFRA-002: API Gateway | monorepo | Fase 4 |
| 94 | INFRA-003: Prometheus/Grafana | monorepo | Fase 4 |
| 95 | INFRA-004: Zipkin | monorepo | Fase 4 |
| 96 | INFRA-005: build centralizado | monorepo | Fase 4 |
| 97 | INFRA-006: docker-compose com servicos | monorepo | Fase 4 |
| 98 | AUTH-005: verificacao de email | monorepo | Fase 4 |
| 99 | AUTH-006: reset de senha | monorepo | Fase 4 |
| 100 | GAME-001: IA de oponente | frontend | Fase 4 |

**Tempo estimado:** 4-6 semanas

---

## Resumo Visual

```
FASE 0 ─── 2-3 dias  ─── Fundacao (compila + conecta)
   │
   ▼
FASE 1 ─── 1-2 sem  ─── MVP Jogavel (login → criar → jogar → resultado)
   │
   ▼
FASE 2 ─── 1-2 sem  ─── Regras Completas (sem cheat, side/extra/banimento)
   │
   ▼
FASE 3 ─── 2-3 sem  ─── Multiplayer Real (matchmaking + Kafka + desafios)
   │
   ▼
FASE 4 ─── 2-3 sem  ─── Qualidade (testes, fallback, CI/CD, infra)
   │
   ▼
FASE 5 ─── 4-6 sem  ─── Infinity (motion, som, onboarding, gateway, IA)
```

**Tempo total estimado:** 3-4 meses com 1 dev full-time.
**Pela primeira vez jogavel:** Final da Fase 1 (~2 semanas).
