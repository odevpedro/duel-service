package com.odevpedro.yugiohcollections.duel.application.service;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;

public interface DuelApplicationService {
    DuelResponse createDuel(CreateDuelRequest request);
    DuelState findById(String duelId);
    void endDuel(String duelId, String winnerId);
}
