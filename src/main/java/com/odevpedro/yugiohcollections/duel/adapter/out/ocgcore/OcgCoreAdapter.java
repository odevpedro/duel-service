package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.Card;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.Zone;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardPosition;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardType;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            log.info("OCG Bridge response: {}", resultJson.substring(0, Math.min(resultJson.length(), 2000)));
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

        syncFieldPositions(state, engine.getField(), resp.getCardData());
    }

    @SuppressWarnings("unchecked")
    private void syncFieldPositions(DuelState state, Object fieldObj, Object cardDataObj) {
        if (fieldObj == null) return;
        try {
            Map<String, Object> field = (Map<String, Object>) fieldObj;
            List<Map<String, Object>> players = (List<Map<String, Object>>) field.get("players");
            if (players == null || players.size() < 2) return;

            List<Map<String, Object>> cardPlayers = null;
            if (cardDataObj instanceof Map) {
                cardPlayers = (List<Map<String, Object>>) ((Map<String, Object>) cardDataObj).get("players");
            }

            Player[] javaPlayers = {state.getPlayerA(), state.getPlayerB()};

            for (int idx = 0; idx < 2; idx++) {
                Map<String, Object> fp = players.get(idx);
                Player p = javaPlayers[idx];
                if (fp == null || p == null) continue;

                syncZoneList(p.getMonsterZones(),
                        (List<Map<String, Object>>) fp.get("monsterZones"),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("monsterZones") : null);
                syncZoneList(p.getSpellTrapZones(),
                        (List<Map<String, Object>>) fp.get("spellTrapZones"),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("spellTrapZones") : null);

                syncPile(p.getHand(),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("hand") : null);
                syncPile(p.getGraveyard(),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("grave") : null);
                syncPile(p.getBanished(),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("removed") : null);
                syncPile(p.getDeck(),
                        cardPlayers != null ? (List<Object>) cardPlayers.get(idx).get("deck") : null);
            }
        } catch (Exception e) {
            log.warn("Failed to sync field positions: {}", e.getMessage());
        }
    }

    private void syncZoneList(List<Zone> zones, List<Map<String, Object>> fieldZones, List<Object> cardDataZones) {
        if (fieldZones == null || zones == null) return;
        for (int i = 0; i < Math.min(zones.size(), fieldZones.size()); i++) {
            Map<String, Object> fz = fieldZones.get(i);
            Zone z = zones.get(i);
            if (fz == null || z == null) continue;
            boolean present = Boolean.TRUE.equals(fz.get("present"));

            if (!present && (cardDataZones == null || i >= cardDataZones.size() || cardDataZones.get(i) == null)) {
                z.setCard(null);
                z.setPosition(CardPosition.ATTACK);
            } else {
                int pos = asInt(fz.get("position"));
                z.setPosition(mapCardPosition(pos));

                if (cardDataZones != null && i < cardDataZones.size()) {
                    Object cardObj = cardDataZones.get(i);
                    if (cardObj instanceof Map) {
                        Map<String, Object> cm = (Map<String, Object>) cardObj;
                        z.setCard(buildCardFromCode(cm));
                    }
                }
            }
        }
    }

    private void syncPile(List<Card> pile, List<Object> cardData) {
        if (pile == null) return;
        pile.clear();
        if (cardData == null) return;
        for (Object obj : cardData) {
            if (obj instanceof Map) {
                Map<String, Object> cm = (Map<String, Object>) obj;
                pile.add(buildCardFromCode(cm));
            } else {
                pile.add(null);
            }
        }
    }

    private Card buildCardFromCode(Map<String, Object> cm) {
        long code = asLong(cm.get("code"));
        String cardId = String.valueOf(code);
        CardType type = CardType.MONSTER;
        if (cm.containsKey("type")) {
            int rawType = asInt(cm.get("type"));
            if ((rawType & 0x2) != 0) type = CardType.SPELL;
            else if ((rawType & 0x4) != 0) type = CardType.TRAP;
        }
        return Card.builder()
                .cardId(cardId)
                .name("Card " + code)
                .code(code)
                .type(type)
                .atk(asInt(cm.getOrDefault("atk", 0)))
                .def(asInt(cm.getOrDefault("def", 0)))
                .level(asInt(cm.getOrDefault("level", 0)))
                .build();
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

    private int asInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number n) return n.longValue();
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
