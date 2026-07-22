# Direcao do Ecossistema para Agentes

## Objetivo Atual

O unico marco de produto em andamento e uma partida local completa no
navegador contra WindBot, usando ocgcore, CardScripts e cards.cdb reais.

Antes de alterar gameplay:

1. Leia `docs/local-duel-backlog.md`.
2. Leia `runtime/README.md`.
3. Confirme o fluxo em `http://localhost:5173/duel/local`.

O backlog canonico e a fonte de verdade. Documentos antigos descrevem o
Spring/JNI e os microsservicos apenas como arquitetura legada ou futura.

## Arquitetura do Duelo Local

```text
React original (/duel/local)
  -> protocolo EDOPro/YGOPro binario por WebSocket
Evolution Server
  -> CoreIntegrator
ocgcore + CardScripts + cards.cdb
  <- WindBot oficial como segundo jogador
```

- O React apresenta estado e envia escolhas oferecidas pelo protocolo.
- O ocgcore e a unica autoridade de regras, efeitos, fases, dano e correntes.
- O WindBot toma as decisoes da IA. Nao ha IA Java ou React.
- Imagens e metadados dos decks de smoke sao locais durante a partida.

## Projetos e Responsabilidades

| Projeto | Caminho | Responsabilidade atual |
|---|---|---|
| duel-service | `./` | Runtime Evolution/ocgcore/WindBot, scripts e backlog canonico |
| frontend | `../yu-gi-oh-deck-management-front-end/yugioh-duel-react/yugioh-duel-react/` | Campo React e cliente binario em `/duel/local` |
| deck-management | `../yu-gi-oh-deck-management/` | Cadastro, catalogo, decks e recursos sociais; fora do inner loop local |

O deck-management nao foi descartado. O deck-service voltara a fornecer decks
quando a partida fixa estiver consolidada. Auth, historico, comunidade e Kafka
continuam sendo recursos do produto, mas nao podem bloquear o smoke local.

## Regras Obrigatorias

- Nao criar stubs, mocks de engine, `LocalEngine` ou fallback de regras.
- Nao implementar regras de Yu-Gi-Oh! em Java, JavaScript ou React.
- Nao tratar `build_response()` ou respostas automaticas genericas como IA.
- Nao iniciar Spring Boot, auth, Redis, Kafka, PostgreSQL, card-service ou
  deck-service para testar `/duel/local`.
- Nao adicionar JWT ao fluxo local.
- Nao buscar imagem em endpoint REST durante o duelo de smoke.
- Nao esconder o inspetor de carta com prompts ou menus de acao.
- Acoes ligadas a cartas e zonas visiveis devem acontecer no proprio campo.
- Uma corrente sem candidatos pode ser recusada automaticamente. Havendo uma
  resposta legal opcional, o jogador deve poder ativar ou escolher
  `Nao responder`. Correntes forcadas nao podem ser puladas.
- Motion design deve representar eventos confirmados pelo ocgcore e nunca
  adiantar ou inventar estado.

## Desenvolvimento Rapido

Fluxo completo minimo:

```bash
./dev.sh local-play
```

Comandos especializados:

```bash
./dev.sh runtime-setup   # baixa e verifica recursos fixados
./dev.sh runtime-up      # recompila/reinicia Evolution e WindBot
./dev.sh runtime-status
./dev.sh runtime-down
```

Alteracao apenas no frontend usa o Vite ja ativo; nao reconstrua containers.
Alteracao em `runtime/evolution`, Compose ou Dockerfile exige `runtime-up`.

## Builds e Testes

- Nunca execute testes unitarios no inner loop.
- Java: `./gradlew build -x test` ou `./dev.sh compile`.
- Frontend original: `npm run build` no diretorio do app React.
- Cliente isolado: `npm run build` em `runtime/web-client`.
- Nao rode `npm install` se `node_modules` e o lockfile ja satisfazem o build.
- Valide mudancas visuais em desktop e mobile com navegador real.
- Para gameplay, o aceite e o fluxo jogavel local, nao apenas compilacao.

## Criterio de Smoke

O smoke deve confirmar, conforme a area alterada:

- sala criada sem login;
- deck humano Blue-Eyes com 40 cartas e Extra Deck real;
- WindBot entra como oponente e executa combo;
- summon, set, ativacao, selecao, corrente, batalha e game over passam pelo
  protocolo e pelo ocgcore;
- nenhuma informacao privada do oponente e revelada;
- nenhuma chamada a auth, deck-service, card-service ou API externa de imagem;
- nenhum erro de pagina ou pacote de protocolo ignorado.

Nao marque itens do backlog como concluidos sem evidencia correspondente.

## Portas do Fluxo Atual

| Servico | Porta |
|---|---:|
| frontend Vite | 5173 |
| Evolution WebSocket | 4000/4001 |
| Evolution HTTP | 7922 |
| WindBot launcher | 2399 |

As portas 8080-8086 pertencem aos servicos legados/futuros e nao participam do
duelo local minimo.
