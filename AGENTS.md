# Contexto do Ecossistema para Agentes

## Objetivo Atual

O unico marco de produto em andamento e uma partida local completa no
navegador contra WindBot, usando ocgcore, CardScripts e cards.cdb reais.

- Leia `docs/local-duel-backlog.md` antes de alterar gameplay.
- Nao implemente regras de duelo em Java ou React.
- Nao trate `build_response()` como IA.
- Nao use ou recrie stubs, mocks de engine ou fallbacks de regras.
- Nao execute auth-service, Redis, Kafka ou outros microsservicos para validar
  o fluxo local.
- Nao marque gameplay como concluido sem executar o smoke manual E2E-001.

## Projetos

Este repositório (`duel-service`) faz parte de um ecossistema de 3 projetos:

| Projeto | Caminho | Função | Porta |
|---|---|---|---|
| duel-service | `./` | Motor de duelo (Spring Boot + ocgcore C++ via JNI) | 8084 |
| yu-gi-oh-deck-management | `../yu-gi-oh-deck-management/` | 6 microsserviços backend (auth, card, deck, card-creator, proxy, community) | 8080-8086 |
| yu-gi-oh-deck-management-front-end | `../yu-gi-oh-deck-management-front-end/` | React SPA (campo de duelo interativo) | 5173 |

## Regras Obrigatórias para Teste

1. **Sempre pular testes unitários**: Use `./gradlew build -x test` em TODOS os builds. Os testes demoram muito e não são necessários para teste local.
2. **Ignorar autenticação**: Durante teste local, TODOS os endpoints devem ser públicos (permitAll). Auth-service é usado apenas para registro/login, mas os demais serviços NÃO devem validar JWT.
3. **Usar ocgcore nativo sempre**: O stub OcgCoreStub foi removido. O motor C++ ocgcore via JNI é a ÚNICA implementação disponível. O profile `dev` agora também usa o motor nativo.

## Stack

- **Java 21** (ambos backends), **Gradle** (wrapper incluso)
- **Spring Boot 3.2**, **PostgreSQL 16**, **Redis 7**, **Kafka**
- **Node 18** + **React 18** + **Vite 5** (front-end)
- **C++20** + **CMake 3.20+** (ygopro-core via JNI)

## Otimizações para Teste Local

### Build rápido (pular testes)
```bash
./gradlew build -x test
```
Sempre use `-x test` durante desenvolvimento/teste local. Os testes são extensos e demoram. O build completo sem testes leva <10s.

### Para subir full stack rapidamente

```bash
# 0. Matar processos antigos (NÃO usar pkill - trava o terminal)
kill $(ps aux | grep bootRun | grep -v grep | awk '{print $2}') 2>/dev/null; sleep 2

# 1. Infraestrutura (apenas o necessário)
cd ../yu-gi-oh-deck-management && docker compose up -d deck-db auth-db
cd ../duel-service && docker compose up -d redis

# 2. Build sem testes
cd ../yu-gi-oh-deck-management
./gradlew :shared-domain:build -x test
./gradlew :auth-service:build :card-service:build :deck-service:build -x test

cd ../duel-service
./gradlew build -x test

# 3. Iniciar serviços (em terminais separados ou background)
cd ../yu-gi-oh-deck-management
nohup ./gradlew :auth-service:bootRun > logs/auth-service.log 2>&1 &
nohup ./gradlew :card-service:bootRun > logs/card-service.log 2>&1 &
nohup ./gradlew :deck-service:bootRun > logs/deck-service.log 2>&1 &

cd ../duel-service
nohup ./gradlew bootRun --args='--spring.profiles.active=local' > /tmp/duel-logs/duel-service.log 2>&1 &

# 4. Front-end
cd ../yu-gi-oh-deck-management-front-end/yugioh-duel-react/yugioh-duel-react
npm install && npm run dev
```

### Verificação rápida de saúde
```bash
for p in 8080 8081 8084 8086; do
  echo "port $p: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$p/actuator/health 2>&1)"
done
```

### Perfis do duel-service
- Todos os perfis usam ocgcore nativo (C++ via JNI). O stub foi removido.
- `dev` (padrão): H2, Redis opcional
- `local`: Redis opcional, Kafka com fallback rápido
- `prod`: Redis obrigatório, Kafka real

## Problemas Comuns e Correções

### CORS
Resolvido permanentemente: todos os serviços têm `.cors(cors -> {})` configurado. Não é mais necessário configurar CORS manualmente ou usar `--disable-web-security`.

### Erro: TokenValidationClient not found
Faltam os pacotes do shared-domain no `@EnableFeignClients`. Corrigir com:
```java
@EnableFeignClients(basePackages = {"seu.pacote", "com.odevpedro.yugiohcollections.shared.security"})
@ComponentScan(basePackages = {"seu.pacote", "com.odevpedro.yugiohcollections.shared"})
```

### Erro: duplicate libocgcore.so no bootJar
Adicionar no `build.gradle`:
```groovy
bootJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

### Portas dos serviços
| Serviço | Porta | Docker DB | Porta DB |
|---|---|---|---|
| card-service | 8080 | - | - |
| deck-service | 8081 | deck-db (postgres:16) | 5433 |
| proxy-service | 8082 | - | - |
| card-creator-service | 8083 | creator-db (postgres:16) | 5434 |
| duel-service | 8084 | redis:7 | 6379 |
| community-service | 8085 | community-db (postgis:16) | 5436 |
| auth-service | 8086 | auth-db (postgres:16) | 5435 |
| front-end | 5173 | - | - |

## Fluxo de Teste Completo

1. Abrir `http://localhost:5173` no navegador
2. Registrar usuário (Register) com username, email, senha (auth-service ainda funciona)
3. Fazer login (o front-end gerencia o token)
4. No lobby, criar um deck via API (sem token necessário):
   ```bash
   DECK_ID=$(curl -s -X POST http://localhost:8081/decks \
     -H 'Content-Type: application/json' \
     -d '{"name":"deck-teste"}' | jq -r '.id')

   for card in 46986414 89631139 78033274; do
     curl -s -X POST "http://localhost:8081/decks/$DECK_ID/cards" \
       -H 'Content-Type: application/json' \
       -d "{\"cardId\": $card, \"quantity\": 3, \"zone\": \"MAIN\"}"
   done
   ```
5. Recarregar o lobby e selecionar o deck
6. Criar duelo contra "ai"
7. Jogar! O bot agora summona monstros, ataca e ativa cartas automaticamente.
   As ações são processadas pelo motor C++ ocgcore via JNI.
