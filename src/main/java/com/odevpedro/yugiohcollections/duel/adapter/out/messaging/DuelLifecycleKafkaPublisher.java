package com.odevpedro.yugiohcollections.duel.adapter.out.messaging;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.event.DuelEncerradoEvent;
import com.odevpedro.yugiohcollections.duel.domain.model.event.DuelStartedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DuelLifecycleKafkaPublisher {

    public static final String DUEL_STARTED_TOPIC = "duel.iniciado";
    public static final String DUEL_FINISHED_TOPIC = "duel.encerrado";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DuelLifecycleKafkaPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDuelStarted(DuelState state) {
        publish(DUEL_STARTED_TOPIC, state.getDuelId(), new DuelStartedEvent(
                state.getDuelId(),
                state.getPlayerAId(),
                state.getPlayerBId(),
                state.getCreatedAt() != null ? state.getCreatedAt() : LocalDateTime.now()
        ));
    }

    public void publishDuelFinished(DuelState state) {
        String loserId = state.getWinnerId() == null ? null
                : (state.getWinnerId().equals(state.getPlayerAId()) ? state.getPlayerBId() : state.getPlayerAId());

        publish(DUEL_FINISHED_TOPIC, state.getDuelId(), new DuelEncerradoEvent(
                state.getDuelId(),
                state.getWinnerId(),
                loserId,
                state.getPlayerAId(),
                state.getPlayerBId(),
                state.getTurnNumber(),
                state.getUpdatedAt() != null ? state.getUpdatedAt() : LocalDateTime.now()
        ));
    }

    private void publish(String topic, String key, Object payload) {
        if (kafkaTemplate == null) {
            log.trace("Kafka disabled, skipping publish to {}", topic);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.warn("Kafka publish failed for topic {}: {}", topic, e.getMessage());
        }
    }
}
