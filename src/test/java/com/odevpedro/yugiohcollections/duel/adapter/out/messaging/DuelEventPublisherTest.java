package com.odevpedro.yugiohcollections.duel.adapter.out.messaging;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DuelEventPublisherTest {

    @Test
    void shouldPublishStateUpdateAndGameOverMessages() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        DuelEventPublisher publisher = new DuelEventPublisher(template);

        DuelState state = DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(Player.builder().playerId("player-a").lifePoints(8000).build())
                .playerB(Player.builder().playerId("player-b").lifePoints(8000).build())
                .currentPhase(Phase.MAIN_1)
                .status(GameStatus.IN_PROGRESS)
                .build();

        publisher.publishStateUpdate("duel-1", state);
        publisher.publishGameOver("duel-1", "player-a");
        publisher.publishPlayerDisconnected("duel-1", "player-b", 180);
        publisher.publishPlayerReconnected("duel-1", "player-b");

        verify(template).convertAndSend("/topic/duel/duel-1", state);
        verify(template).convertAndSend("/topic/duel/duel-1/over", java.util.Map.of("type", "GAME_OVER", "winnerId", "player-a"));
    }
}
