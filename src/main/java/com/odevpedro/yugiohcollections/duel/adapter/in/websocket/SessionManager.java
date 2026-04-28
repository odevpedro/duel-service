package com.odevpedro.yugiohcollections.duel.adapter.in.websocket;

import com.odevpedro.yugiohcollections.duel.config.StompPrincipal;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<String, String> sessionToDuel = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String duelId, String playerId) {
        sessionToDuel.put(sessionId, duelId);
        sessionToPlayer.put(sessionId, playerId);
    }

    public void unregisterSession(String sessionId) {
        sessionToDuel.remove(sessionId);
        sessionToPlayer.remove(sessionId);
    }

    public String getDuelId(String sessionId) {
        return sessionToDuel.get(sessionId);
    }

    public String getPlayerId(String sessionId) {
        return sessionToPlayer.get(sessionId);
    }

    public String getSessionIdByPlayer(String duelId, String playerId) {
        for (Map.Entry<String, String> entry : sessionToDuel.entrySet()) {
            if (entry.getValue().equals(duelId)) {
                String sessionId = entry.getKey();
                if (playerId.equals(sessionToPlayer.get(sessionId))) {
                    return sessionId;
                }
            }
        }
        return null;
    }
}