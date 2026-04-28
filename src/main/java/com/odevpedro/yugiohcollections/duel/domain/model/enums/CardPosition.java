package com.odevpedro.yugiohcollections.duel.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CardPosition {
    ATTACK,
    DEFENSE_FACE_UP,
    DEFENSE_FACE_DOWN;

    @JsonValue
    public String toValue() {
        return name();
    }
}
