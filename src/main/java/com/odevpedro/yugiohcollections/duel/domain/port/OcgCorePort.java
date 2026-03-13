package com.odevpedro.yugiohcollections.duel.domain.port;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;

public interface OcgCorePort {
    DuelState processAction(DuelState state, DuelActionDTO action, String playerId);
    DuelState advancePhase(DuelState state);
    boolean isActionValid(DuelState state, DuelActionDTO action, String playerId);
}
