package com.odevpedro.yugiohcollections.duel.application.mapper;

import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.entity.DuelHistoryEntity;
import com.odevpedro.yugiohcollections.duel.application.dto.history.DuelHistoryResponse;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DuelHistoryMapperTest {

    private final DuelHistoryMapper mapper = new DuelHistoryMapper();

    @Test
    void toEntityShouldMapDuelStateToEntity() {
        LocalDateTime now = LocalDateTime.now();
        
        DuelState state = DuelState.builder()
                .duelId("duel-123")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(0).build())
                .turnNumber(5)
                .status(GameStatus.FINISHED)
                .currentPhase(Phase.END)
                .createdAt(now.minusMinutes(10))
                .updatedAt(now)
                .build();

        DuelHistoryEntity entity = mapper.toEntity(state, "player-a");

        assertThat(entity.getDuelId()).isEqualTo("duel-123");
        assertThat(entity.getPlayerAId()).isEqualTo("player-a");
        assertThat(entity.getPlayerBId()).isEqualTo("player-b");
        assertThat(entity.getWinnerId()).isEqualTo("player-a");
        assertThat(entity.getLoserId()).isEqualTo("player-b");
        assertThat(entity.getPlayerAFinalLp()).isEqualTo(8000);
        assertThat(entity.getPlayerBFinalLp()).isEqualTo(0);
        assertThat(entity.getTurnCount()).isEqualTo(5);
        assertThat(entity.getResult()).isEqualTo("COMPLETED");
        assertThat(entity.getDurationSeconds()).isGreaterThan(0);
    }

    @Test
    void toEntityShouldHandleNullWinner() {
        DuelState state = DuelState.builder()
                .duelId("duel-456")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(4000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(4000).build())
                .status(GameStatus.FINISHED)
                .build();

        DuelHistoryEntity entity = mapper.toEntity(state, null);

        assertThat(entity.getWinnerId()).isNull();
        assertThat(entity.getLoserId()).isNull();
        assertThat(entity.getResult()).isEqualTo("DRAW");
    }

    @Test
    void toResponseShouldMapEntityToResponse() {
        LocalDateTime now = LocalDateTime.now();
        
        DuelHistoryEntity entity = DuelHistoryEntity.builder()
                .id(1L)
                .duelId("duel-123")
                .playerAId("player-a")
                .playerBId("player-b")
                .winnerId("player-a")
                .loserId("player-b")
                .playerAFinalLp(8000)
                .playerBFinalLp(0)
                .turnCount(5)
                .result("COMPLETED")
                .startedAt(now.minusMinutes(10))
                .finishedAt(now)
                .durationSeconds(600L)
                .build();

        DuelHistoryResponse response = mapper.toResponse(entity);

        assertThat(response.getDuelId()).isEqualTo("duel-123");
        assertThat(response.getWinnerId()).isEqualTo("player-a");
        assertThat(response.getLoserId()).isEqualTo("player-b");
        assertThat(response.getPlayerAFinalLp()).isEqualTo(8000);
        assertThat(response.getPlayerBFinalLp()).isEqualTo(0);
        assertThat(response.getTurnCount()).isEqualTo(5);
        assertThat(response.getResult()).isEqualTo("COMPLETED");
        assertThat(response.getDurationSeconds()).isEqualTo(600L);
    }
}