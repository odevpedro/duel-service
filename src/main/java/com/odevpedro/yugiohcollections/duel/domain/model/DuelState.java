package com.odevpedro.yugiohcollections.duel.domain.model;

import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DuelState {
    private String duelId;
    private Phase currentPhase;
    private int turnNumber;
    private String activePlayerId;
    private Player playerA;
    private Player playerB;
    private GameStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Player getOpponent(String playerId) {
        return playerA.getPlayerId().equals(playerId) ? playerB : playerA;
    }

    public Player getActivePlayer() {
        return playerA.getPlayerId().equals(activePlayerId) ? playerA : playerB;
    }
}
