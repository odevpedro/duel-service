package com.odevpedro.yugiohcollections.duel.application.mapper;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import org.springframework.stereotype.Component;

@Component
public class DuelMapper {

    public DuelResponse toResponse(DuelState state) {
        return DuelResponse.builder()
                .duelId(state.getDuelId())
                .playerAId(state.getPlayerA().getPlayerId())
                .playerBId(state.getPlayerB().getPlayerId())
                .currentPhase(state.getCurrentPhase())
                .status(state.getStatus())
                .turnNumber(state.getTurnNumber())
                .activePlayerId(state.getActivePlayerId())
                .build();
    }
}
