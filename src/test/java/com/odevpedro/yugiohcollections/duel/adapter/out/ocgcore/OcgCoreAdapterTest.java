package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcgCoreAdapterTest {

    @Test
    void shouldFallBackToStubWhenNativeLibraryIsNotLoaded() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", false);
        OcgCoreBridge bridge = mock(OcgCoreBridge.class);
        OcgCoreAdapter adapter = new OcgCoreAdapter(bridge, new ObjectMapper());

        DuelState state = state();
        DuelActionDTO action = action("SUMMON");

        assertThat(adapter.isActionValid(state, action, "player-a")).isTrue();
    }

    @Test
    void shouldUseBridgeWhenNativeLibraryIsLoaded() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", true);
        OcgCoreBridge bridge = mock(OcgCoreBridge.class);
        ObjectMapper mapper = new ObjectMapper();
        OcgCoreAdapter adapter = new OcgCoreAdapter(bridge, mapper);

        DuelState state = state();
        DuelActionDTO action = action("SUMMON");
        when(bridge.isActionValid(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("player-a")))
                .thenReturn(true);
        try {
            when(bridge.processAction(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("player-a")))
                    .thenReturn(mapper.writeValueAsString(state));
            when(bridge.advancePhase(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(mapper.writeValueAsString(state));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(adapter.isActionValid(state, action, "player-a")).isTrue();
        assertThat(adapter.processAction(state, action, "player-a")).isNotNull();
        assertThat(adapter.advancePhase(state)).isNotNull();
    }

    @Test
    void shouldReturnFalseWhenBridgeThrowsDuringValidation() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", true);
        OcgCoreBridge bridge = mock(OcgCoreBridge.class);
        ObjectMapper mapper = new ObjectMapper();
        OcgCoreAdapter adapter = new OcgCoreAdapter(bridge, mapper);

        DuelState state = state();
        DuelActionDTO action = action("SUMMON");

        assertThatThrownBy(() -> adapter.processAction(state, action, "player-a")).isInstanceOf(RuntimeException.class);
        assertThat(adapter.isActionValid(state, action, "player-a")).isFalse();
    }

    @Test
    void shouldPropagateBridgeJsonErrorsAsRuntimeException() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", true);
        OcgCoreBridge bridge = mock(OcgCoreBridge.class);
        ObjectMapper mapper = new ObjectMapper();
        OcgCoreAdapter adapter = new OcgCoreAdapter(bridge, mapper);

        DuelState state = state();
        DuelActionDTO action = action("SUMMON");

        when(bridge.processAction(anyString(), anyString(), org.mockito.ArgumentMatchers.eq("player-a")))
                .thenReturn("{broken-json");

        assertThatThrownBy(() -> adapter.processAction(state, action, "player-a"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ocgcore error");
    }

    @Test
    void shouldUseBridgeForAdvancePhaseWhenLoaded() throws Exception {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", true);
        OcgCoreBridge bridge = mock(OcgCoreBridge.class);
        ObjectMapper mapper = new ObjectMapper();
        OcgCoreAdapter adapter = new OcgCoreAdapter(bridge, mapper);

        DuelState state = state();
        when(bridge.advancePhase(anyString())).thenReturn(mapper.writeValueAsString(state));

        assertThat(adapter.advancePhase(state)).isNotNull();
        verify(bridge).advancePhase(anyString());
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

    private DuelActionDTO action(String type) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId("duel-1");
        action.setActionType(type);
        action.setCardId("card-1");
        return action;
    }
}
