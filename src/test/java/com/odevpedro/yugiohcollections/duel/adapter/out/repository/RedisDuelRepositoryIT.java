package com.odevpedro.yugiohcollections.duel.adapter.out.repository;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisDuelRepositoryIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Test
    void shouldSaveLoadAndDeleteDuelState() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);

        RedisDuelRepository repository = new RedisDuelRepository(template, 1L);

        DuelState state = DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();

        repository.save(state);

        assertThat(repository.findById("duel-1")).isPresent();
        repository.delete("duel-1");
        assertThat(repository.findById("duel-1")).isEmpty();

        connectionFactory.destroy();
    }

    @Test
    void shouldExpireDuelStateWhenTtlElapses() throws Exception {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);

        RedisDuelRepository repository = new RedisDuelRepository(template, 1L);

        DuelState state = DuelState.builder()
                .duelId("duel-expire")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .activePlayerId("player-a")
                .turnNumber(1)
                .build();

        repository.save(state);
        Thread.sleep(1200);

        assertThat(repository.findById("duel-expire")).isEmpty();
        connectionFactory.destroy();
    }
}
