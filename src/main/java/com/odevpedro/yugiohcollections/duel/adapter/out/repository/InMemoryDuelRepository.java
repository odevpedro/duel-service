package com.odevpedro.yugiohcollections.duel.adapter.out.repository;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDuelRepository implements DuelRepositoryPort {

    private final Map<String, DuelState> store = new ConcurrentHashMap<>();

    @Override
    public DuelState save(DuelState duelState) {
        store.put(duelState.getDuelId(), duelState);
        return duelState;
    }

    @Override
    public Optional<DuelState> findById(String duelId) {
        return Optional.ofNullable(store.get(duelId));
    }

    @Override
    public void delete(String duelId) {
        store.remove(duelId);
    }
}
