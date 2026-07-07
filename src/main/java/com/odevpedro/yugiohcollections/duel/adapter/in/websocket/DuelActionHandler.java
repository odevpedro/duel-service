package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.application.dto.PhaseChangeDTO;
import com.odevpedro.yugiohcollections.duel.application.service.ActionService;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.config.StompPrincipal;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import jakarta.validation.Valid;
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
    public void handleAction(@Valid @Payload DuelActionDTO action, Principal principal) {
        DuelState updated = actionService.process(action, resolvePlayerId(principal));
        publisher.publishStateUpdate(action.getDuelId(), updated);
        publishGameOverIfNeeded(action.getDuelId(), updated);
    }

    @MessageMapping("/duel.phase")
    public void handlePhaseChange(@Valid @Payload PhaseChangeDTO dto, Principal principal) {
        DuelState state = duelService.findById(dto.getDuelId());
        DuelState updated = phaseService.advance(state);
        publisher.publishStateUpdate(dto.getDuelId(), updated);
        publishGameOverIfNeeded(dto.getDuelId(), updated);
    }

    private String resolvePlayerId(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getUserId();
        }
        return principal.getName();
    }

    private void publishGameOverIfNeeded(String duelId, DuelState state) {
        if (state.getStatus() == GameStatus.FINISHED && state.getWinnerId() != null) {
            publisher.publishGameOver(duelId, state.getWinnerId());
        }
    }
}
