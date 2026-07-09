package com.odevpedro.yugiohcollections.duel.application.service;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotPlayerService {

    public static final String BOT_ID = "ai";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "bot-player");
        t.setDaemon(true);
        return t;
    });

    public void maybeAutoPlay(String duelId, Runnable playTurn) {
        scheduler.schedule(() -> {
            try {
                playTurn.run();
            } catch (Exception e) {
                log.warn("[BOT] error: {}", e.getMessage());
            }
        }, 1200, TimeUnit.MILLISECONDS);
    }
}
