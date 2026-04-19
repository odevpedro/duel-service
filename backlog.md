# Backlog — duel-service

> Registrovivo do progresso do projeto. Atualizado a cada mudança de estado de uma funcionalidade.
> **Última atualização:** 2025-04-17

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

> Features atualmente sendo desenvolvidas.

- `[~]` Autenticação JWT em handshake WebSocket — P0 — L

---

## Pendentes

> Ordenadas por prioridade. Itens de P0 e P1 devem entrar em "Em Andamento" primeiro.

**Infraestrutura**

- `[ ]` Migração para Redis (persistent duel state) — P1 — L

**Integrações**

- `[ ]` Card database integration with card-creator-service — P2 — M
- `[ ]` Duel history and result persistence — P2 — M

**Funcionalidades**

- `[ ]` Disconnect handling — notificar oponente e pausar duelo — P1 — M

---

## Concluídas

> Features finalizadas com suas respectivas datas de conclusão e links de referência.

- `[x]` Criação de duelos via REST API — 2025-01 — PR #1
- `[x]` WebSocket STOMP para comunicação em tempo real — 2025-01 — PR #2
- `[x]` Integração com ocgcore via JNI — 2025-02 — PR #3
- `[x]` Gerenciamento de fases do duelo — 2025-02 — PR #4
- `[x]` Sistema de ações (SUMMON, ATTACK, SPELL, SET) — 2025-03 — PR #5

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

- Decidir estratégia de persistência de estado: Redis vs PostgreSQL
- Definir formato de storage para histórico de duelos
-_CONFIGURAR authentication WebSocket com auth-service

---

## Histórico de Versões

| Versão | Data | Principais entregas |
|--------|------|---------------------|
| `0.0.1-SNAPSHOT` | 2025-04-17 | MVP com funcionalidades core |