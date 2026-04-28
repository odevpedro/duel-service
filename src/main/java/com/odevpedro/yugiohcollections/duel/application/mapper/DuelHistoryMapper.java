package com.odevpedro.yugiohcollections.duel.application.mapper;

import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.entity.DuelHistoryEntity;
import com.odevpedro.yugiohcollections.duel.application.dto.history.DuelHistoryResponse;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DuelHistoryMapper {

    public DuelHistoryEntity toEntity(DuelState state, String winnerId) {
        String loserId = winnerId != null 
            ? (state.getPlayerAId().equals(winnerId) ? state.getPlayerBId() : state.getPlayerAId())
            : null;

        Integer playerAFinalLp = state.getPlayerA() != null ? state.getPlayerA().getLifePoints() : null;
        Integer playerBFinalLp = state.getPlayerB() != null ? state.getPlayerB().getLifePoints() : null;

        Long duration = state.getCreatedAt() != null && state.getUpdatedAt() != null
            ? Duration.between(state.getCreatedAt(), state.getUpdatedAt()).getSeconds()
            : null;

        return DuelHistoryEntity.builder()
                .duelId(state.getDuelId())
                .playerAId(state.getPlayerAId())
                .playerBId(state.getPlayerBId())
                .winnerId(winnerId)
                .loserId(loserId)
                .playerAFinalLp(playerAFinalLp)
                .playerBFinalLp(playerBFinalLp)
                .turnCount(state.getTurnNumber())
                .result(winnerId != null ? "COMPLETED" : "DRAW")
                .startedAt(state.getCreatedAt())
                .finishedAt(state.getUpdatedAt())
                .durationSeconds(duration)
                .build();
    }

    public DuelHistoryResponse toResponse(DuelHistoryEntity entity) {
        return DuelHistoryResponse.builder()
                .duelId(entity.getDuelId())
                .playerAId(entity.getPlayerAId())
                .playerBId(entity.getPlayerBId())
                .winnerId(entity.getWinnerId())
                .loserId(entity.getLoserId())
                .playerAFinalLp(entity.getPlayerAFinalLp())
                .playerBFinalLp(entity.getPlayerBFinalLp())
                .turnCount(entity.getTurnCount())
                .duelType(entity.getDuelType())
                .result(entity.getResult())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .durationSeconds(entity.getDurationSeconds())
                .build();
    }
}