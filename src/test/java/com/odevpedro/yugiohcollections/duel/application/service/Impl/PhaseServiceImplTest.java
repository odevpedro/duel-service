package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhaseServiceImplTest {

    @Mock private OcgCorePort ocgCore;
    @Mock private DuelRepositoryPort repository;

    private PhaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PhaseServiceImpl(ocgCore, repository);
    }

    @Test
    void advanceShouldPersistUpdatedState() {
        DuelState state = state(Phase.END);
        DuelState updated = state(Phase.DRAW);
        updated.setUpdatedAt(java.time.LocalDateTime.now());

        when(ocgCore.advancePhase(any())).thenReturn(updated);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DuelState result = service.advance(state);

        assertThat(result.getCurrentPhase()).isEqualTo(Phase.DRAW);
    }

    @Test
    void shouldAllowActionsByPhase() {
        assertThat(service.isActionAllowed(Phase.MAIN_1, "SUMMON")).isTrue();
        assertThat(service.isActionAllowed(Phase.BATTLE, "ATTACK")).isTrue();
        assertThat(service.isActionAllowed(Phase.DRAW, "SUMMON")).isFalse();
    }

    private DuelState state(Phase phase) {
        return DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(phase)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();
    }
}
