package com.odevpedro.yugiohcollections.duel.domain.model;

import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardPosition;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.ZoneType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Zone {
    private int index;
    private ZoneType type;
    private Card card;
    private CardPosition position;

    public boolean isEmpty() {
        return card == null;
    }
}
