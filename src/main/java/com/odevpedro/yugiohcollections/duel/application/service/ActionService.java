package com.odevpedro.yugiohcollections.duel.application.service;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;

public interface ActionService {
    DuelState process(DuelActionDTO action, String playerId);
    DuelState summon(DuelState state, String playerId, String cardId, int zoneIndex);
    DuelState attack(DuelState state, String attackerId, String targetId);
    DuelState activateSpell(DuelState state, String playerId, String cardId);
}
