package com.odevpedro.yugiohcollections.duel.adapter.out.external;

import lombok.Data;

@Data
public class DeckCardSummaryDTO {
    private Long cardId;
    private String name;
    private String type;
    private String imageUrl;
    private String description;
    private Integer atk;
    private Integer def;
    private Integer level;
    private Integer quantity;
}
