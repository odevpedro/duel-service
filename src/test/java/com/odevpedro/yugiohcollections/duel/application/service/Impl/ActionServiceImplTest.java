package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock private OcgCorePort ocgCore;
    @Mock private DuelRepositoryPort repository;
    @Mock private MeterRegistry meterRegistry;

    private ActionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActionServiceImpl(ocgCore, repository, meterRegistry);
    }

    @Test
    void processShouldPersistValidAction() {
        DuelState state = state();
        DuelState updated = state();
        updated.setUpdatedAt(java.time.LocalDateTime.now());

        when(repository.findById("duel-1")).thenReturn(Optional.of(state));
        when(ocgCore.isActionValid(any(), any(), any())).thenReturn(true);
        when(ocgCore.processAction(any(), any(), any())).thenReturn(updated);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DuelState result = service.process(action("SUMMON"), "player-a");

        org.assertj.core.api.Assertions.assertThat(result.getDuelId()).isEqualTo("duel-1");
    }

    @Test
    void processShouldFailWhenActionIsInvalid() {
        when(repository.findById("duel-1")).thenReturn(Optional.of(state()));
        when(ocgCore.isActionValid(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.process(action("ATTACK"), "player-a"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid action");
    }

    @Test
    void processShouldFailWhenDuelIsMissing() {
        when(repository.findById("duel-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(action("ATTACK"), "player-a"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duel not found");
    }

    @Test
    void summonAttackAndActivateSpellShouldDelegateToOcgCore() {
        DuelState state = state();
        when(ocgCore.processAction(any(), any(), any())).thenReturn(state);

        service.summon(state, "player-a", "card-1", 0);
        service.attack(state, "player-a", "target-1");
        service.activateSpell(state, "player-a", "spell-1");

        ArgumentCaptor<DuelActionDTO> captor = ArgumentCaptor.forClass(DuelActionDTO.class);
        verify(ocgCore, times(3)).processAction(any(), captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues())
                .extracting(DuelActionDTO::getActionType, DuelActionDTO::getCardId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SUMMON", "card-1"),
                        org.assertj.core.groups.Tuple.tuple("ATTACK", null),
                        org.assertj.core.groups.Tuple.tuple("SPELL", "spell-1")
                );
    }

    private DuelState state() {
        return DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();
    }

    private DuelActionDTO action(String actionType) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId("duel-1");
        action.setActionType(actionType);
        action.setCardId("card-1");
        return action;
    }

}
