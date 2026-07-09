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
    private String playerAId;
    private String playerBId;
    private Long playerADeckId;
    private Long playerBDeckId;
    private Phase currentPhase;
    private int turnNumber;
    private String activePlayerId;
    private Player playerA;
    private Player playerB;
    private GameStatus status;
    private String winnerId;
    private String victoryType;
    private String duelType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean firstTurn;

    private String disconnectedPlayerId;
    private LocalDateTime disconnectedAt;
    private long version;

    public Player getOpponent(String playerId) {
        return playerA.getPlayerId().equals(playerId) ? playerB : playerA;
    }

    public Player getActivePlayer() {
        return playerA.getPlayerId().equals(activePlayerId) ? playerA : playerB;
    }

    public boolean hasDisconnected() {
        return disconnectedPlayerId != null;
    }
}
