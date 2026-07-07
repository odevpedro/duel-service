package com.odevpedro.yugiohcollections.duel.domain;

import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerTest {

    @Test
    void shouldNeverDropLifePointsBelowZeroWhenTakingDamage() {
        Player player = Player.builder()
                .playerId("player-a")
                .lifePoints(1000)
                .build();

        player.takeDamage(1500);

        assertThat(player.getLifePoints()).isZero();
        assertThat(player.isAlive()).isFalse();
    }

    @Test
    void shouldNormalizeDirectLifePointAssignmentsBelowZero() {
        Player player = Player.builder()
                .playerId("player-a")
                .lifePoints(1000)
                .build();

        player.setLifePoints(-300);

        assertThat(player.getLifePoints()).isZero();
    }

    @Test
    void shouldGainLifePoints() {
        Player player = Player.builder()
                .playerId("player-a")
                .lifePoints(1000)
                .build();

        player.gainLife(500);

        assertThat(player.getLifePoints()).isEqualTo(1500);
    }
}
