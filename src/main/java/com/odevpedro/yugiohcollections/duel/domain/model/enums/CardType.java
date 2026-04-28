package com.odevpedro.yugiohcollections.duel.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CardType {
    MONSTER,
    SPELL,
    TRAP;

    @JsonValue
    public String toValue() {
        return name();
    }
}
