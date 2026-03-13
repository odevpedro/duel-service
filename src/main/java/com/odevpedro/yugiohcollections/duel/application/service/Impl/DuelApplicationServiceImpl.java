package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DuelApplicationServiceImpl implements DuelApplicationService {

    private static final int INITIAL_LIFE_POINTS = 8000;

    private final DuelRepositoryPort repository;
    private final DuelMapper mapper;

    @Override
    public DuelResponse createDuel(CreateDuelRequest request) {
        Player playerA = Player.builder()
                .playerId(request.getPlayerAId())
                .lifePoints(INITIAL_LIFE_POINTS)
                .build();

        Player playerB = Player.builder()
                .playerId(request.getPlayerBId())
                .lifePoints(INITIAL_LIFE_POINTS)
                .build();

        DuelState state = DuelState.builder()
                .duelId(UUID.randomUUID().toString())
                .playerA(playerA)
                .playerB(playerB)
                .currentPhase(Phase.DRAW)
                .turnNumber(1)
                .activePlayerId(request.getPlayerAId())
                .status(GameStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mapper.toResponse(repository.save(state));
    }

    @Override
    public DuelState findById(String duelId) {
        return repository.findById(duelId)
                .orElseThrow(() -> new RuntimeException("Duel not found: " + duelId));
    }

    @Override
    public void endDuel(String duelId, String winnerId) {
        DuelState state = findById(duelId);
        state.setStatus(GameStatus.FINISHED);
        state.setUpdatedAt(LocalDateTime.now());
        repository.save(state);
    }
}
