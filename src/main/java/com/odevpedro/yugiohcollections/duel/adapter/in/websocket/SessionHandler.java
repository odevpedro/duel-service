package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionHandler {

    private static final int DISCONNECT_TIMEOUT_SECONDS = 180;

    private final DuelEventPublisherPort publisher;
    private final DuelRepositoryPort repository;
    private final SessionManager sessionManager;
    private final DuelHistoryRepository historyRepository;
    private final DuelHistoryMapper historyMapper;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.info("Player connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("Player disconnected: sessionId={}", sessionId);

        String duelId = sessionManager.getDuelId(sessionId);
        String playerId = sessionManager.getPlayerId(sessionId);

        sessionManager.unregisterSession(sessionId);

        if (duelId == null || playerId == null) {
            log.warn("No duel found for session: {}", sessionId);
            return;
        }

        handlePlayerDisconnect(duelId, playerId);
    }

    @Async
    public void handlePlayerDisconnect(String duelId, String disconnectedPlayerId) {
        DuelState state = repository.findById(duelId).orElse(null);
        if (state == null || state.getStatus() != GameStatus.IN_PROGRESS) {
            return;
        }

        String opponentId = state.getOpponent(disconnectedPlayerId).getPlayerId();

        state.setDisconnectedPlayerId(disconnectedPlayerId);
        state.setDisconnectedAt(LocalDateTime.now());
        repository.save(state);

        publisher.publishPlayerDisconnected(duelId, disconnectedPlayerId, DISCONNECT_TIMEOUT_SECONDS);

        log.info("Player {} disconnected in duel {}. Opponent {} will win in {} seconds if not reconnected.",
                disconnectedPlayerId, duelId, opponentId, DISCONNECT_TIMEOUT_SECONDS);

        handleDisconnectTimeoutAsync(duelId, disconnectedPlayerId, opponentId, DISCONNECT_TIMEOUT_SECONDS);
    }

    @Async
    public void handlePlayerReconnect(String duelId, String reconnectedPlayerId) {
        DuelState state = repository.findById(duelId).orElse(null);
        if (state == null) {
            return;
        }

        if (reconnectedPlayerId.equals(state.getDisconnectedPlayerId())) {
            state.setDisconnectedPlayerId(null);
            state.setDisconnectedAt(null);
            repository.save(state);

            publisher.publishPlayerReconnected(duelId, reconnectedPlayerId);
            log.info("Player {} reconnected to duel {}", reconnectedPlayerId, duelId);
        }
    }

    private void handleDisconnectTimeoutAsync(String duelId, String disconnectedPlayerId, String opponentId, int delaySeconds) {
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DuelState state = repository.findById(duelId).orElse(null);
        if (state != null &&
            state.getDisconnectedPlayerId() != null &&
            state.getDisconnectedPlayerId().equals(disconnectedPlayerId) &&
            state.getStatus() == GameStatus.IN_PROGRESS) {

            log.info("Player {} did not reconnect in time. Opponent {} wins by WO.", disconnectedPlayerId, opponentId);

            state.setStatus(GameStatus.FINISHED);
            state.setUpdatedAt(LocalDateTime.now());
            
            var historyEntity = historyMapper.toEntity(state, opponentId);
            historyRepository.save(historyEntity);
            
            repository.save(state);

            publisher.publishGameOver(duelId, opponentId);
        }
    }
}
