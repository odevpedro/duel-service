package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    void shouldRegisterAndRetrieveSession() {
        sessionManager.registerSession("session-1", "duel-123", "player-a");

        assertThat(sessionManager.getDuelId("session-1")).isEqualTo("duel-123");
        assertThat(sessionManager.getPlayerId("session-1")).isEqualTo("player-a");
    }

    @Test
    void shouldUnregisterSession() {
        sessionManager.registerSession("session-1", "duel-123", "player-a");
        sessionManager.unregisterSession("session-1");

        assertThat(sessionManager.getDuelId("session-1")).isNull();
        assertThat(sessionManager.getPlayerId("session-1")).isNull();
    }

    @Test
    void shouldFindSessionIdByPlayer() {
        sessionManager.registerSession("session-1", "duel-123", "player-a");
        sessionManager.registerSession("session-2", "duel-123", "player-b");

        String sessionId = sessionManager.getSessionIdByPlayer("duel-123", "player-a");
        assertThat(sessionId).isEqualTo("session-1");

        sessionId = sessionManager.getSessionIdByPlayer("duel-123", "player-b");
        assertThat(sessionId).isEqualTo("session-2");
    }

    @Test
    void shouldReturnNullForNonExistentSession() {
        assertThat(sessionManager.getDuelId("non-existent")).isNull();
        assertThat(sessionManager.getPlayerId("non-existent")).isNull();
        assertThat(sessionManager.getSessionIdByPlayer("duel-123", "player-x")).isNull();
    }
}