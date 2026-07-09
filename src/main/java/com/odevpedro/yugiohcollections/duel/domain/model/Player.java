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
    private List<Card> banished = new ArrayList<>();

    @Builder.Default
    private List<Card> extraDeck = new ArrayList<>();

    @Builder.Default
    private List<Card> sideDeck = new ArrayList<>();

    @Builder.Default
    private List<Zone> monsterZones = new ArrayList<>();

    @Builder.Default
    private List<Zone> spellTrapZones = new ArrayList<>();

    public void setLifePoints(int lifePoints) {
        this.lifePoints = Math.max(0, lifePoints);
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative");
        }
        setLifePoints(lifePoints - amount);
    }

    public void gainLife(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Life gain amount cannot be negative");
        }
        lifePoints += amount;
    }

    public boolean isAlive() {
        return lifePoints > 0;
    }
}
