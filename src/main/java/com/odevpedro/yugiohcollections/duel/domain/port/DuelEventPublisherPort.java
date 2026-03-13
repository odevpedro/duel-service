package com.odevpedro.yugiohcollections.duel.domain.port;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;

public interface DuelEventPublisherPort {
    void publishStateUpdate(String duelId, DuelState state);
    void publishGameOver(String duelId, String winnerId);
}
