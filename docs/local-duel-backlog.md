# Backlog canonico - duelo local jogavel

> Fonte de verdade para o trabalho de gameplay a partir de 2026-07-21.
> O backlog antigo continua no repositorio apenas como historico.

## Objetivo

Entregar uma partida local completa no navegador contra uma IA real do
ecossistema YGOPro:

- ocgcore nativo como unica autoridade das regras;
- CardScripts Lua e cards.cdb canonicos;
- WindBot como jogador virtual;
- navegador como cliente de apresentacao e entrada;
- imagens servidas de cache local;
- nenhum stub, regra simulada ou resposta automatica fingindo ser IA.

## Nao objetivos desta fase

- login, registro ou validacao JWT;
- matchmaking e comunidade;
- Redis, Kafka ou persistencia de historico;
- criacao e edicao de decks pela aplicacao;
- deploy de producao;
- aumentar cobertura unitaria;
- implementar regras de Yu-Gi-Oh! em Java ou React.

Esses recursos permanecem no ecossistema, mas nao bloqueiam o marco jogavel.

## Arquitetura alvo

```text
React original (`/duel/local`)
  | protocolo EDOPro/YGOPro binario por WebSocket
  v
Evolution Server (TypeScript)
  | processo CoreIntegrator
  v
ocgcore + CardScripts + cards.cdb
  ^
  | protocolo EDOPro/YGOPro
WindBot
```

O React nao decide acoes legais, dano, chains, fases ou efeitos. O WindBot nao
roda dentro do bridge JNI e nao e substituido por heuristicas Java.

## Estado real em 2026-07-21

| Area | Estado |
|---|---|
| runtime nativo | Evolution v2.13.2 com ocgcore carregado |
| recursos | CardScripts, BabelCDB e LFLists em commits fixados |
| deck humano | Blue-Eyes 2025, 40 Main e 15 Extra |
| IA | WindBot oficial com executor Blue-Eyes |
| prompts humanos | Main Phase, chain, carta, zona, posicao e opcoes integrados |
| frontend local | `/duel/local` no frontend original, sem LocalEngine |
| imagens | cache local preenchido no setup |
| smoke real | efeito, alvo/zona, fim de turno e turno do bot comprovados |
| game over | validado ate `0 LP`, `Win` e encerramento pelo ocgcore |

Nenhuma tarefa pode ser marcada como concluida apenas porque o core compilou.

## Proxima ordem de trabalho

1. Fechar o unico item manual do marco jogavel: ataque iniciado pelo humano.
2. Implementar fila visual e motion dos eventos confirmados pelo ocgcore.
3. Gravar transcript e automatizar o smoke curto para impedir regressoes.
4. Tornar a velocidade do oponente configuravel na propria experiencia local.
5. Reintegrar primeiro o deck-service, mantendo auth e demais servicos fora do
   fluxo local anonimo.

## Regras de implementacao

1. O runtime deve falhar se ocgcore, cards.cdb ou scripts nao estiverem presentes.
2. Nunca adicionar fallback para stub ou regras simplificadas.
3. Escolhas humanas devem corresponder aos prompts emitidos pelo core.
4. O bot deve ser o WindBot conectado como jogador da sala.
5. Informacao privada deve ser filtrada pela perspectiva do jogador.
6. O fluxo local nao pode depender de auth-service, card-service ou deck-service.
7. Builds locais nao executam testes unitarios.
8. O core nativo so e recompilado quando C++ ou sua versao fixada mudar.

## Marco 0 - ciclo local rapido

### DEV-001 - Compilacao Java incremental

- [x] Criar `./gradlew localBuild`, dependente apenas de `classes`.
- [x] Ativar daemon, cache de build, VFS watch e execucao paralela do Gradle.
- [x] Medir build inicial e build incremental: 9,69 s na primeira compilacao
  com novo daemon e 1,51 s na repeticao `UP-TO-DATE`, em 2026-07-21.

Aceite: alteracao Java recompila sem testes, `bootJar` ou tarefas de
empacotamento.

### DEV-002 - Build nativo incremental

- [x] Usar `cmake --build ... --parallel`.
- [x] Fixar Evolution/core por commit e verificar o binario por checksum.
- [x] Separar `runtime-setup` do comando cotidiano `local-play`.
- [ ] Disponibilizar artefato nativo precompilado para o ambiente local.

Aceite: mudanca fora do C++ nao recompila ocgcore.

### DEV-003 - Runtime local minimo

- [x] Desabilitar JWT do WebSocket no profile `local`.
- [x] Falhar no startup quando a biblioteca nativa nao carregar.
- [x] Criar usuario local fixo `local-player` no frontend.
- [x] Subir apenas runtime de duelo e Vite.
- [x] Remover auth, Redis, Kafka, H2 e demais microsservicos do comando local.

Aceite: comando unico deixa backend e frontend prontos sem login ou
infraestrutura externa.

### DEV-004 - Comandos canonicos

- [x] Criar `dev.sh` para compile, package, native e run.
- [x] Substituir `start-test.sh` por um launcher do fluxo minimo quando o
  runtime web estiver conectado.

Aceite: agentes e desenvolvedores nao precisam memorizar flags de teste.

## Marco 1 - baseline upstream comprovada

### RUNTIME-001 - Servidor de salas real

- [x] Usar o Evolution Server TypeScript em commit fixado.
- [x] Dispensar Multirole apos a compatibilidade do Evolution ser comprovada.
- [x] Subir o servidor com ocgcore, CardScripts, BabelCDB e banlist compativeis.
- [x] Registrar versoes, commits e checksums dos recursos.

Aceite: o cliente EDOPro original entra em uma sala e completa um turno sem
erros de script ou protocolo.

### RUNTIME-002 - WindBot real

- [x] Fixar uma versao do ProjectIgnis/WindBot.
- [x] Escolher um executor e deck conhecidos, inicialmente Blue-Eyes ou
  Normal Monster Mash.
- [x] Fazer o bot entrar na sala como segundo jogador.
- [x] Retirar `BotPlayerService` e `build_response()` do fluxo local real.

Aceite: EDOPro original contra WindBot completa uma partida. O log comprova
summons, ativacao de efeito, chain, batalha e game over.

### RUNTIME-003 - Recursos de carta

- [x] Fixar banco, scripts e core em revisoes conhecidas.
- [x] Validar scripts base e utilitarios no startup.
- [x] Falhar com diagnostico claro quando um recurso estiver ausente.
- [x] Usar decks `.ydk` fixos e versionados para o smoke local.

Aceite: ao menos uma carta de efeito do deck humano e uma do bot resolvem seus
efeitos integralmente.

## Marco 2 - adaptador para navegador

### PROTO-001 - Contrato de eventos

- [x] Consumir o protocolo binario tipado com `ygopro-msg-encode` no browser.
- [x] Preservar tipo, jogador, localizacao e sequencia das mensagens do core.
- [x] Nao reduzir o protocolo a `SUMMON`, `ATTACK`, `SPELL` e `SET`.
- [ ] Salvar transcript deterministico de uma partida de referencia.

Aceite: todos os eventos da partida EDOPro de referencia podem ser reproduzidos
e inspecionados.

### PROTO-002 - Prompts e respostas

- [x] Expor ao browser os prompts usados pelo deck de smoke com suas opcoes legais.
- [x] Correlacionar resposta com jogador e prompt pendente no Evolution.
- [x] Codificar escolhas simples e selecoes progressivas no formato moderno.
- [x] Rejeitar resposta atrasada, duplicada ou de outro jogador.

Aceite: humano escolhe zona, posicao, alvo, custo e chain pelo navegador.

### PROTO-003 - Privacidade

- [x] Criar snapshot por perspectiva.
- [x] Ocultar mao, deck e cartas baixadas do oponente.
- [x] Expor codigo apenas quando o protocolo revelar a carta.

Aceite: nenhuma mensagem WebSocket revela informacao privada.

## Marco 3 - experiencia local no React

### WEB-LOCAL-001 - Entrada direta

- [x] `/duel/local` cria ou entra na sala como `local-player`.
- [x] Remover dependencia de token no modo local.
- [x] Pular lobby, login, deck-service e matchmaking.

### WEB-LOCAL-002 - UI orientada por prompts

- [x] Renderizar estado recebido, sem mutar regras localmente.
- [x] Gerar botoes e selecoes apenas a partir do prompt atual.
- [x] Desativar `LocalEngine`, `actionResolver` e IA React no fluxo real.
- [x] Manter animacoes como consequencia dos eventos confirmados pelo servidor.

### WEB-LOCAL-003 - Imagens locais

- [x] Resolver imagens por `/local-assets/cards/<passcode>.jpg`.
- [x] Baixar uma vez somente as imagens dos decks de smoke.
- [x] Servir assets estaticos com cache longo.
- [x] Usar card back local para informacao oculta.

Aceite do marco: nenhuma consulta ao card-service ou API externa durante uma
partida com decks de smoke.

## Marco 4 - partida jogavel

### E2E-001 - Smoke manual obrigatorio

- [x] Iniciar tudo com um comando.
- [x] Abrir diretamente a partida local.
- [x] Comprar, ativar efeito e responder selecoes de carta e zona.
- [x] Executar summon e entrar em battle com o humano.
- [ ] Confirmar manualmente um ataque iniciado pelo humano.
- [x] WindBot executar ao menos uma sequencia propria do deck.
- [x] Partida chegar a game over pelo ocgcore.
- [x] Reiniciar em um lobby novo depois do game over.

### E2E-002 - Regressao deterministica

- [ ] Fixar decks, seed, core, scripts e banco.
- [ ] Gravar transcript de uma partida curta.
- [ ] Criar smoke automatico apenas para startup, conexao e primeiro prompt.
- [ ] Executar suite completa somente sob demanda ou CI.

### WEB-LOCAL-004 - Acoes contextuais no campo

- [x] Manter o inspetor de carta disponivel no hover e fixa-lo no clique.
- [x] Exibir summon, set, ativacao e ataque diretamente na carta ou zona legal.
- [x] Mostrar a corrente em uma faixa compacta que nao substitui o inspetor.
- [x] Reservar o painel lateral exclusivamente para o inspetor de carta.
- [x] Preservar os controles contextuais em viewports de toque.

Aceite: o fluxo principal nao depende de uma lista lateral e nenhuma decisao
oculta as informacoes da carta inspecionada.

### WEB-LOCAL-005 - Selecao espacial e prioridade de corrente

- [x] Responder `SELECT_EFFECT_YN` diretamente na carta que oferece o efeito.
- [x] Exibir opcoes reais de `SELECT_CHAIN` sobre as cartas correspondentes.
- [x] Responder automaticamente uma corrente vazia, sem interromper o jogador.
- [x] Manter `Nao responder` quando ha efeito legal e a corrente nao e forcada.
- [x] Selecionar cartas visiveis e zonas diretamente nos elementos do campo.
- [x] Ampliar o inspetor local, a arte e o texto de efeito para leitura continua.

Aceite: o jogador so e interrompido por uma corrente quando existe uma resposta
legal; escolhas ligadas a uma carta ou zona visivel acontecem no proprio campo.

## Marco 5 - ritmo e motion design

### UX-MOTION-001 - Ritmo legivel do oponente

- [x] Adicionar atraso configuravel somente entre decisoes do WindBot.
- [ ] Exibir um estado visual discreto enquanto o WindBot decide.
- [ ] Criar uma fila visual para eventos consecutivos sem bloquear o protocolo.
- [ ] Permitir velocidade normal, rapida e instantanea nas configuracoes locais.

### UX-MOTION-002 - Cartas com presenca fisica

- [x] Ampliar o foco da carta na mao no hover e manter foco menor no clique.
- [ ] Animar compra da pilha para a mao a partir do evento confirmado.
- [ ] Animar summon, set, flip e ativacao entre origem e zona de destino.
- [ ] Animar envio ao cemiterio, banimento e retorno para a mao/deck.
- [ ] Criar transicoes de reorganizacao da mao sem deslocamentos bruscos.

### UX-MOTION-003 - Combate, corrente e acessibilidade

- [ ] Representar ataque, alvo, dano e alteracao de LP com timing coordenado.
- [ ] Representar elos e resolucao de corrente sem cobrir o inspetor.
- [ ] Dar feedback visual para efeitos negados, alvos e cartas reveladas.
- [ ] Respeitar `prefers-reduced-motion` e oferecer reducao manual de movimento.
- [ ] Garantir que motion nunca altere estado antes da confirmacao do ocgcore.

Aceite do marco: uma sequencia do WindBot pode ser acompanhada visualmente,
evento por evento, e o jogador consegue inspecionar a carta relevante durante
toda a resolucao.

## Marco 6 - reintegracao gradual

Somente depois de E2E-001:

O deck-management permanece como sistema de cadastro, catalogo e decks. Ele
nao participa do inner loop atual, mas tambem nao deve ser removido ou duplicado
dentro do runtime de duelo.

- [ ] carregar decks do deck-service;
- [ ] registrar resultado e historico;
- [ ] publicar eventos Kafka;
- [ ] reativar autenticacao fora do profile local;
- [ ] adicionar multiplayer e matchmaking;
- [ ] decidir quais partes do Spring continuam necessarias.

## Definicao de jogavel

O projeto e jogavel somente quando todos estes pontos forem verdadeiros:

- startup sem stub e sem fallback;
- dois decks reais carregados pelo runtime;
- scripts Lua executando;
- humano responde prompts pelo navegador;
- WindBot executa estrategia de um executor real;
- estado e informacao privada permanecem corretos;
- partida termina pelo ocgcore;
- imagens dos decks de teste funcionam sem rede;
- todo o fluxo inicia sem auth e sem executar testes unitarios.

## Estrategia de testes

| Momento | Validacao |
|---|---|
| Alteracao Java | `./dev.sh compile` |
| Empacotamento | `./dev.sh package` |
| Alteracao no bridge | `./dev.sh native` |
| Alteracao de protocolo | replay/transcript fixo |
| Alteracao visual | partida local manual |
| Marco concluido | E2E-001 completo |
| CI ou sob demanda | testes unitarios existentes |

## Verificacao de 2026-07-21

- `npm run build` do cliente isolado: sucesso;
- `npm run build` do frontend original: sucesso;
- `/duel/local`: seis cartas reais distintas na mao e dez acoes legais em uma
  Main Phase observada;
- efeito de `Maiden of White`: ativado, carta e zona selecionadas, resolvido
  pelo ocgcore;
- turno encerrado, WindBot executou sua jogada e o controle voltou ao humano;
- partida completa: eventos `Attack`, `Battle`, `Damage` e `Win`, com LP em
  `8000 x 0` e encerramento confirmado pelo ocgcore;
- fluxo humano entrou na Battle Phase e, depois do encerramento, a acao
  `JOGAR NOVAMENTE` abriu um lobby novo;
- nenhum pacote de protocolo ignorado e nenhum erro de pagina durante o smoke
  completo;
- layout validado em 1600x1000 e 390x844 sem overflow horizontal;
- acoes de summon, set, ativacao e ataque ancoradas nas cartas do prompt;
- inspetor preservado durante a Main Phase e controles dentro do viewport em
  1600x1000 e 390x844;
- inspetor local medido em 390 px, arte em 300 px e texto de efeito em 12,48 px
  com linha de 19,72 px no viewport 1600x1000;
- ativacoes reais de `Bingo Machine, Go!!!` e `Maiden of White` chegaram a
  selecao de zona ancorada no campo;
- correntes sem nenhuma opcao legal sao recusadas automaticamente; correntes
  opcionais com candidatos preservam a escolha `Nao responder`;
- WindBot com atraso local de 650 ms: intervalos de 582 ms e 1.073 ms observados
  entre blocos consecutivos do combo;
- carta focada na mao: caixa visual de 239x341 px no desktop e 204x291 px no
  mobile, com controles acima da carta e sem overflow horizontal;
- nenhum teste unitario foi executado;

- `bash -n dev.sh`: sucesso;
- `./dev.sh compile`: 9,69 s com novo daemon e 1,51 s incremental;
- `./dev.sh package`: 2,57 s, com testes excluidos;
- profile `local`: startup em 4,412 s na porta temporaria 18084;
- `GET /actuator/health`: HTTP 200 com status `UP`;
- biblioteca `libocgcore.so`: carregada com sucesso;
- nenhum teste unitario foi executado.

Testes unitarios nao sao apagados. Eles deixam de fazer parte do inner loop.
