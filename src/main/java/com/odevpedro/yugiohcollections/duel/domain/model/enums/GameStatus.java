package com.odevpedro.yugiohcollections.duel.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GameStatus {
    WAITING,
    IN_PROGRESS,
    FINISHED;

    @JsonValue
    public String toValue() {
        return name();
    }
}
