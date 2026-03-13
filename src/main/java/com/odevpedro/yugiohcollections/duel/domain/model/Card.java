package com.odevpedro.yugiohcollections.duel.domain.model;

import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Card {
    private String cardId;
    private String name;
    private int atk;
    private int def;
    private int level;
    private CardType type;
}
