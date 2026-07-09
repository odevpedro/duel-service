package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownNotifier {

    private final SessionManager sessionManager;
    private final com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort repository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        for (String duelId : sessionManager.getActiveDuelIds()) {
            DuelState state = repository.findById(duelId).orElse(null);
            if (state == null || state.getStatus() == null) {
                continue;
            }

            messagingTemplate.convertAndSend("/topic/duel/" + duelId, Map.of(
                    "type", "SERVER_SHUTTING_DOWN",
                    "duelId", duelId,
                    "status", state.getStatus().name()
            ));
            log.info("Notified duel {} about server shutdown", duelId);
        }
    }
}
