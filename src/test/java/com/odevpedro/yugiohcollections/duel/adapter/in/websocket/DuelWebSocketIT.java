package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odevpedro.yugiohcollections.duel.DuelServiceApplication;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.adapter.out.messaging.DuelEventPublisher;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = DuelServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("dev")
class DuelWebSocketIT {

    @Autowired
    private DuelEventPublisher eventPublisher;

    @Autowired
    private DuelRepositoryPort repository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldConnectAndReceiveDuelStateUpdates() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token("player-a", "player-a", "PLAYER"));

        AtomicReference<DuelState> received = new AtomicReference<>();

        String url = "ws://localhost:" + port + "/ws";
        StompSession session = client.connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            }
        }).get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/duel/duel-1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.set(new ObjectMapper().convertValue(payload, DuelState.class));
            }
        });

        DuelState state = DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();

        eventPublisher.publishStateUpdate("duel-1", state);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (received.get() == null && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getDuelId()).isEqualTo("duel-1");
        session.disconnect();
    }

    @Test
    void shouldRejectConnectionWithoutToken() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), new StompHeaders(), new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS)
        ).isInstanceOf(Exception.class);
    }

    @Test
    void shouldProcessSummonActionOverWebSocket() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());

        DuelState state = DuelState.builder()
                .duelId("duel-2")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder()
                        .playerId("player-a")
                        .lifePoints(8000)
                        .hand(java.util.List.of(card("card-1")))
                        .monsterZones(java.util.List.of(zone(0)))
                        .spellTrapZones(java.util.List.of(zone(0)))
                        .build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();
        repository.save(state);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token("player-a", "player-a", "PLAYER"));

        AtomicReference<DuelState> received = new AtomicReference<>();
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
        session.subscribe("/topic/duel/duel-2", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.set(new ObjectMapper().convertValue(payload, DuelState.class));
            }
        });

        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId("duel-2");
        action.setActionType("SUMMON");
        action.setCardId("card-1");
        action.setZoneIndex(0);
        session.send("/app/duel.action", action);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (received.get() == null && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getPlayerA().getMonsterZones().get(0).getCard().getCardId()).isEqualTo("card-1");
        session.disconnect();
    }

    private String token(String userId, String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private com.odevpedro.yugiohcollections.duel.domain.model.Card card(String id) {
        return com.odevpedro.yugiohcollections.duel.domain.model.Card.builder()
                .cardId(id)
                .name(id)
                .atk(1000)
                .def(1000)
                .level(4)
                .build();
    }

    private com.odevpedro.yugiohcollections.duel.domain.model.Zone zone(int index) {
        return com.odevpedro.yugiohcollections.duel.domain.model.Zone.builder()
                .index(index)
                .build();
    }
}
