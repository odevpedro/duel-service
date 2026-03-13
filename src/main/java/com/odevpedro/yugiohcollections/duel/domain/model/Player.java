package com.odevpedro.yugiohcollections.duel.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Player {
    private String playerId;
    private int lifePoints;

    @Builder.Default
    private List<Card> hand = new ArrayList<>();

    @Builder.Default
    private List<Card> deck = new ArrayList<>();

    @Builder.Default
    private List<Card> graveyard = new ArrayList<>();

    @Builder.Default
    private List<Zone> monsterZones = new ArrayList<>();

    @Builder.Default
    private List<Zone> spellTrapZones = new ArrayList<>();

    public boolean isAlive() {
        return lifePoints > 0;
    }
}
