package com.odevpedro.yugiohcollections.duel.adapter.out.external;

import lombok.Data;

import java.util.List;

@Data
public class DeckViewResponse {
    private Long id;
    private String ownerId;
    private String name;
    private List<DeckCardSummaryDTO> cards;
    private List<DeckCardSummaryDTO> mainDeckCards;
    private List<DeckCardSummaryDTO> extraDeckCards;
    private List<DeckCardSummaryDTO> sideDeckCards;
    private int totalCards;
    private String notes;
    private int mainDeckSize;
    private int extraDeckSize;
    private int sideDeckSize;
    private boolean isValid;
    private List<String> validationErrors;
}
