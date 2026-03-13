package com.odevpedro.yugiohcollections.duel.domain;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuelStateTest {

    @Test
    void shouldReturnOpponentOfActivePlayer() {
        Player playerA = Player.builder().playerId("a").lifePoints(8000).build();
        Player playerB = Player.builder().playerId("b").lifePoints(8000).build();

        DuelState state = DuelState.builder()
                .playerA(playerA)
                .playerB(playerB)
                .activePlayerId("a")
                .build();

        assertThat(state.getOpponent("a").getPlayerId()).isEqualTo("b");
        assertThat(state.getOpponent("b").getPlayerId()).isEqualTo("a");
    }

    @Test
    void shouldReturnActivePlayer() {
        Player playerA = Player.builder().playerId("a").lifePoints(8000).build();
        Player playerB = Player.builder().playerId("b").lifePoints(8000).build();

        DuelState state = DuelState.builder()
                .playerA(playerA)
                .playerB(playerB)
                .activePlayerId("a")
                .build();

        assertThat(state.getActivePlayer().getPlayerId()).isEqualTo("a");
    }
}
