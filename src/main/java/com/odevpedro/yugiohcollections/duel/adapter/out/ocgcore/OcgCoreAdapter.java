package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.Card;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.Zone;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardPosition;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcgCoreAdapter implements OcgCorePort {

    private final OcgCoreBridge bridge;
    private final ObjectMapper objectMapper;
    private final OcgCoreStub fallback = new OcgCoreStub();

    @Override
    public DuelState processAction(DuelState state, DuelActionDTO action, String playerId) {
        try {
            if (!OcgCoreLoader.isLoaded()) {
                return fallback.processAction(state, action, playerId);
            }
            String stateJson  = objectMapper.writeValueAsString(state);
            String actionJson = objectMapper.writeValueAsString(action);

            String resultJson = bridge.processAction(stateJson, actionJson, playerId);
            OcgCoreBridgeResponse resp = objectMapper.readValue(resultJson, OcgCoreBridgeResponse.class);
            applyEngineResult(state, resp);
            return state;
        } catch (Exception e) {
            log.error("ocgcore processAction failed", e);
            throw new RuntimeException("ocgcore error: " + e.getMessage(), e);
        }
    }

    @Override
    public DuelState advancePhase(DuelState state) {
        try {
            if (!OcgCoreLoader.isLoaded()) {
                return fallback.advancePhase(state);
            }
            String stateJson  = objectMapper.writeValueAsString(state);
            String resultJson = bridge.advancePhase(stateJson);
            OcgCoreBridgeResponse resp = objectMapper.readValue(resultJson, OcgCoreBridgeResponse.class);
            applyEngineResult(state, resp);
            return state;
        } catch (Exception e) {
            log.error("ocgcore advancePhase failed", e);
            throw new RuntimeException("ocgcore error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isActionValid(DuelState state, DuelActionDTO action, String playerId) {
        try {
            if (!OcgCoreLoader.isLoaded()) {
                return fallback.isActionValid(state, action, playerId);
            }
            String stateJson  = objectMapper.writeValueAsString(state);
            String actionJson = objectMapper.writeValueAsString(action);
            return bridge.isActionValid(stateJson, actionJson, playerId);
        } catch (Exception e) {
            log.error("ocgcore isActionValid failed", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void applyEngineResult(DuelState state, OcgCoreBridgeResponse resp) {
        OcgCoreBridgeResponse.EngineResult engine = resp.getEngine();
        if (engine == null) return;

        state.setTurnNumber(engine.getTurn());
        state.setCurrentPhase(mapPhase(engine.getPhase()));

        state.getPlayerA().setLifePoints(engine.getLp0());
        state.getPlayerB().setLifePoints(engine.getLp1());

        state.setActivePlayerId(engine.getTurnPlayer() == 0
                ? state.getPlayerAId()
                : state.getPlayerBId());

        if (engine.isGameOver()) {
            state.setStatus(GameStatus.FINISHED);
            if (engine.getWinnerPlayer() == 0) {
                state.setWinnerId(state.getPlayerAId());
            } else if (engine.getWinnerPlayer() == 1) {
                state.setWinnerId(state.getPlayerBId());
            }
            state.setVictoryType(engine.getWinReason() == 1 ? "NORMAL" : "OTHER");
        } else {
            state.setStatus(GameStatus.IN_PROGRESS);
        }

        syncFieldPositions(state, engine.getField());
    }

    @SuppressWarnings("unchecked")
    private void syncFieldPositions(DuelState state, Object fieldObj) {
        if (fieldObj == null) return;
        try {
            Map<String, Object> field = (Map<String, Object>) fieldObj;
            List<Map<String, Object>> players = (List<Map<String, Object>>) field.get("players");
            if (players == null || players.size() < 2) return;

            Player[] javaPlayers = {state.getPlayerA(), state.getPlayerB()};

            for (int idx = 0; idx < 2; idx++) {
                Map<String, Object> fp = players.get(idx);
                Player p = javaPlayers[idx];
                if (fp == null || p == null) continue;

                syncZoneList(p.getMonsterZones(), (List<Map<String, Object>>) fp.get("monsterZones"));
                syncZoneList(p.getSpellTrapZones(), (List<Map<String, Object>>) fp.get("spellTrapZones"));

                int deckCount = asInt(fp.get("deckCount"));
                int handCount = asInt(fp.get("handCount"));
                int graveCount = asInt(fp.get("graveCount"));
                int removedCount = asInt(fp.get("removedCount"));

                trimList(p.getDeck(), deckCount);
                trimList(p.getHand(), handCount);
                trimList(p.getGraveyard(), graveCount);
                trimList(p.getBanished(), removedCount);
            }
        } catch (Exception e) {
            log.warn("Failed to sync field positions: {}", e.getMessage());
        }
    }

    private void syncZoneList(List<Zone> zones, List<Map<String, Object>> fieldZones) {
        if (fieldZones == null || zones == null) return;
        for (int i = 0; i < Math.min(zones.size(), fieldZones.size()); i++) {
            Map<String, Object> fz = fieldZones.get(i);
            Zone z = zones.get(i);
            if (fz == null || z == null) continue;
            boolean present = Boolean.TRUE.equals(fz.get("present"));
            if (!present) {
                z.setCard(null);
                z.setPosition(CardPosition.ATTACK);
            } else {
                int pos = asInt(fz.get("position"));
                z.setPosition(mapCardPosition(pos));
            }
        }
    }

    private CardPosition mapCardPosition(int pos) {
        if ((pos & 0x4) != 0) return CardPosition.DEFENSE_FACE_DOWN;
        if ((pos & 0x2) != 0) return CardPosition.DEFENSE_FACE_UP;
        return CardPosition.ATTACK;
    }

    private void trimList(List<?> list, int targetSize) {
        if (list == null) return;
        while (list.size() > targetSize) {
            list.remove(list.size() - 1);
        }
        while (list.size() < targetSize) {
            list.add(null);
        }
    }

    private int asInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private Phase mapPhase(int phaseCode) {
        return switch (phaseCode) {
            case 0x01 -> Phase.DRAW;
            case 0x02 -> Phase.STANDBY;
            case 0x04 -> Phase.MAIN_1;
            case 0x08, 0x10, 0x20, 0x40, 0x80 -> Phase.BATTLE;
            case 0x100 -> Phase.MAIN_2;
            case 0x200 -> Phase.END;
            default -> Phase.MAIN_1;
        };
    }
}
