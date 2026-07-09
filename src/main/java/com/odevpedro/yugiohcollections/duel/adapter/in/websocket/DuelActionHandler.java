package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.application.dto.PhaseChangeDTO;
import com.odevpedro.yugiohcollections.duel.application.service.ActionService;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.adapter.out.messaging.DuelLifecycleKafkaPublisher;
import com.odevpedro.yugiohcollections.duel.config.StompPrincipal;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequiredArgsConstructor
public class DuelActionHandler {

    private final ActionService actionService;
    private final PhaseService phaseService;
    private final DuelApplicationService duelService;
    private final DuelEventPublisherPort publisher;
    private final DuelLifecycleKafkaPublisher lifecyclePublisher;
    private final MeterRegistry meterRegistry;
    private final Map<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    @Value("${duel.rate-limit.actions-per-second:10}")
    private int actionsPerSecond;

    @MessageMapping("/duel.action")
    public void handleAction(@Valid @Payload DuelActionDTO action, Principal principal) {
        String playerId = resolvePlayerId(principal);
        ensurePlayerIsPartOfDuel(action.getDuelId(), playerId);
        checkRateLimit(playerId);
        Timer.Sample sample = Timer.start(meterRegistry);
        DuelState updated = actionService.process(action, playerId);
        sample.stop(meterRegistry.timer("duel.websocket.action.latency"));
        publisher.publishStateUpdate(action.getDuelId(), updated);
        publishGameOverIfNeeded(action.getDuelId(), updated);
    }

    @MessageMapping("/duel.phase")
    public void handlePhaseChange(@Valid @Payload PhaseChangeDTO dto, Principal principal) {
        String playerId = resolvePlayerId(principal);
        DuelState state = duelService.findById(dto.getDuelId());
        ensurePlayerCanAdvancePhase(state, playerId);
        checkRateLimit(playerId);
        Timer.Sample sample = Timer.start(meterRegistry);
        DuelState updated = phaseService.advance(state);
        sample.stop(meterRegistry.timer("duel.websocket.phase.latency"));
        publisher.publishStateUpdate(dto.getDuelId(), updated);
        publishGameOverIfNeeded(dto.getDuelId(), updated);
    }

    private String resolvePlayerId(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getUserId();
        }
        return principal.getName();
    }

    private void ensurePlayerIsPartOfDuel(String duelId, String playerId) {
        DuelState state = duelService.findById(duelId);
        ensurePlayerBelongsToState(state, playerId);
    }

    private void ensurePlayerCanAdvancePhase(DuelState state, String playerId) {
        ensurePlayerBelongsToState(state, playerId);
        if (!playerId.equals(state.getActivePlayerId())) {
            throw new IllegalArgumentException("Only the active player can advance the phase");
        }
    }

    private void ensurePlayerBelongsToState(DuelState state, String playerId) {
        boolean participant = playerId != null
                && (playerId.equals(state.getPlayerAId()) || playerId.equals(state.getPlayerBId()));
        if (!participant) {
            throw new IllegalArgumentException("Player is not part of this duel");
        }
    }

    private void checkRateLimit(String playerId) {
        long now = System.currentTimeMillis();
        RateWindow window = rateWindows.computeIfAbsent(playerId, ignored -> new RateWindow(now));
        synchronized (window) {
            if (now - window.windowStart >= 1000L) {
                window.windowStart = now;
                window.count.set(0);
            }
            if (window.count.incrementAndGet() > actionsPerSecond) {
                throw new IllegalStateException("Muitas acoes. Aguarde.");
            }
        }
    }

    private void publishGameOverIfNeeded(String duelId, DuelState state) {
        if (state.getStatus() == GameStatus.FINISHED) {
            lifecyclePublisher.publishDuelFinished(state);
            if (state.getWinnerId() != null) {
                publisher.publishGameOver(duelId, state.getWinnerId());
            }
        }
    }

    private static final class RateWindow {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        private RateWindow(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
