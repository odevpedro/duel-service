package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import lombok.Data;

@Data
public class OcgCoreBridgeResponse {
    private String duelId;
    private int turnNumber;
    private String currentPhase;
    private String status;
    private EngineResult engine;
    private Object cardData;

    @Data
    public static class EngineResult {
        private int turn;
        private int phase;
        private int turnPlayer;
        private int lp0;
        private int lp1;
        private boolean gameOver;
        private int winnerPlayer;
        private int winReason;
        private Object field;
    }
}
