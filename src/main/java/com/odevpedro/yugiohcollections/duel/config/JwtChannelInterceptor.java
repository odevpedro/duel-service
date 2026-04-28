package com.odevpedro.yugiohcollections.duel.config;

import com.odevpedro.yugiohcollections.duel.adapter.in.websocket.SessionManager;
import com.odevpedro.yugiohcollections.duel.adapter.in.websocket.SessionHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProperties jwtProperties;
    private final SessionManager sessionManager;
    private final SessionHandler sessionHandler;

    public JwtChannelInterceptor(JwtProperties jwtProperties, 
                                  SessionManager sessionManager,
                                  SessionHandler sessionHandler) {
        this.jwtProperties = jwtProperties;
        this.sessionManager = sessionManager;
        this.sessionHandler = sessionHandler;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header in WebSocket handshake");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7).trim();

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                );

                accessor.setUser(new StompPrincipal(userId, username, role));
                log.info("WebSocket handshake authenticated for user: {} (id: {})", username, userId);

            } catch (Exception e) {
                log.error("JWT validation failed during WebSocket handshake: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid JWT token");
            }
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/duel/")) {
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
                    String duelId = parts[3];

                    if (accessor.getUser() instanceof StompPrincipal principal) {
                        String playerId = principal.getUserId();
                        
                        var existingSession = sessionManager.getSessionIdByPlayer(duelId, playerId);
                        if (existingSession != null && !existingSession.equals(sessionId)) {
                            sessionHandler.handlePlayerReconnect(duelId, playerId);
                            log.info("Player {} reconnected to duel {} (old session: {}, new: {})", 
                                    playerId, duelId, existingSession, sessionId);
                        }
                        
                        sessionManager.registerSession(sessionId, duelId, playerId);
                        log.info("Session {} subscribed to duel {} for player {}", sessionId, duelId, playerId);
                    }
                }
            }
        }

        if (accessor != null && StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            sessionManager.unregisterSession(sessionId);
            log.info("Session {} unsubscribed", sessionId);
        }

        return message;
    }
}