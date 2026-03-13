package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.application.dto.PhaseChangeDTO;
import com.odevpedro.yugiohcollections.duel.application.service.ActionService;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class DuelActionHandler {

    private final ActionService actionService;
    private final PhaseService phaseService;
    private final DuelApplicationService duelService;
    private final DuelEventPublisherPort publisher;

    @MessageMapping("/duel.action")
    public void handleAction(@Payload DuelActionDTO action, Principal principal) {
        DuelState updated = actionService.process(action, principal.getName());
        publisher.publishStateUpdate(action.getDuelId(), updated);
    }

    @MessageMapping("/duel.phase")
    public void handlePhaseChange(@Payload PhaseChangeDTO dto, Principal principal) {
        DuelState state = duelService.findById(dto.getDuelId());
        DuelState updated = phaseService.advance(state);
        publisher.publishStateUpdate(dto.getDuelId(), updated);
    }
}
