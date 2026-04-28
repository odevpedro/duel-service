package com.odevpedro.yugiohcollections.duel.application.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelHistoryResponse {

    private String duelId;
    private String playerAId;
    private String playerBId;
    private String winnerId;
    private String loserId;
    private Integer playerAFinalLp;
    private Integer playerBFinalLp;
    private Integer turnCount;
    private String duelType;
    private String result;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationSeconds;
}