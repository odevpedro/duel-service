package com.odevpedro.yugiohcollections.duel.adapter.out.messaging;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DuelEventPublisher implements DuelEventPublisherPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishStateUpdate(String duelId, DuelState state) {
        messagingTemplate.convertAndSend("/topic/duel/" + duelId, state);
    }

    @Override
    public void publishGameOver(String duelId, String winnerId) {
        messagingTemplate.convertAndSend("/topic/duel/" + duelId + "/over", winnerId);
    }
}
