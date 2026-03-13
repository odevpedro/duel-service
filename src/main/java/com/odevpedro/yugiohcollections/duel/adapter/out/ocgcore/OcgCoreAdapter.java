package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcgCoreAdapter implements OcgCorePort {

    private final OcgCoreBridge bridge;
    private final ObjectMapper objectMapper;

    @Override
    public DuelState processAction(DuelState state, DuelActionDTO action, String playerId) {
        try {
            String stateJson  = objectMapper.writeValueAsString(state);
            String actionJson = objectMapper.writeValueAsString(action);

            String resultJson = bridge.processAction(stateJson, actionJson, playerId);

            return objectMapper.readValue(resultJson, DuelState.class);
        } catch (Exception e) {
            log.error("ocgcore processAction failed", e);
            throw new RuntimeException("ocgcore error: " + e.getMessage(), e);
        }
    }

    @Override
    public DuelState advancePhase(DuelState state) {
        try {
            String stateJson  = objectMapper.writeValueAsString(state);
            String resultJson = bridge.advancePhase(stateJson);
            return objectMapper.readValue(resultJson, DuelState.class);
        } catch (Exception e) {
            log.error("ocgcore advancePhase failed", e);
            throw new RuntimeException("ocgcore error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isActionValid(DuelState state, DuelActionDTO action, String playerId) {
        try {
            String stateJson  = objectMapper.writeValueAsString(state);
            String actionJson = objectMapper.writeValueAsString(action);
            return bridge.isActionValid(stateJson, actionJson, playerId);
        } catch (Exception e) {
            log.error("ocgcore isActionValid failed", e);
            return false;
        }
    }
}
