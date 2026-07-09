package com.odevpedro.yugiohcollections.duel.domain.model.event;

import java.time.LocalDateTime;

public record DuelStartedEvent(
        String duelId,
        String playerAId,
        String playerBId,
        LocalDateTime startedAt
) {}
