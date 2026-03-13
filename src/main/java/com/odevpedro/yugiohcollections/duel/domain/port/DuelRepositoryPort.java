package com.odevpedro.yugiohcollections.duel.domain.port;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;

import java.util.Optional;

public interface DuelRepositoryPort {
    DuelState save(DuelState duelState);
    Optional<DuelState> findById(String duelId);
    void delete(String duelId);
}
