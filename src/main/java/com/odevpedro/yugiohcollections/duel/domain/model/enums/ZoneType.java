package com.odevpedro.yugiohcollections.duel.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ZoneType {
    MONSTER,
    SPELL_TRAP,
    FIELD;

    @JsonValue
    public String toValue() {
        return name();
    }
}
