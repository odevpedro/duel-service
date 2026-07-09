package com.odevpedro.yugiohcollections.duel.domain.model.event;

import java.time.LocalDateTime;

public record DuelEncerradoEvent(
        String duelId,
        String winnerId,
        String loserId,
        String playerAId,
        String playerBId,
        Integer turnCount,
        LocalDateTime finishedAt
) {}
