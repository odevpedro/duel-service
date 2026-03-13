package com.odevpedro.yugiohcollections.duel.application.dto;

import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuelResponse {
    private String duelId;
    private String playerAId;
    private String playerBId;
    private Phase currentPhase;
    private GameStatus status;
    private int turnNumber;
    private String activePlayerId;
}
