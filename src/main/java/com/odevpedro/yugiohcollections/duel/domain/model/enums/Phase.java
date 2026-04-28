package com.odevpedro.yugiohcollections.duel.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Phase {
    DRAW,
    STANDBY,
    MAIN_1,
    BATTLE,
    MAIN_2,
    END;

    @JsonValue
    public String toValue() {
        return name();
    }
}
