package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.domain.port.DuelEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionHandler {

    private final DuelEventPublisherPort publisher;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        log.info("Player connected: {}", event.getUser() != null ? event.getUser().getName() : "unknown");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("Player disconnected: {}", event.getUser() != null ? event.getUser().getName() : "unknown");
        // TODO: notify opponent and handle duel interruption
    }
}
