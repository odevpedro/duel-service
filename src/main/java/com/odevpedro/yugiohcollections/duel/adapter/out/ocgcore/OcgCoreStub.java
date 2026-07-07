package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@Profile("dev")
public class OcgCoreStub implements OcgCorePort {

    @Override
    public DuelState processAction(DuelState state, DuelActionDTO action, String playerId) {
        log.info("[STUB] processAction: {} by {}", action.getActionType(), playerId);
        String actionType = action.getActionType() != null ? action.getActionType().toUpperCase() : "";
        Player player = getPlayer(state, playerId);
        if (player == null) {
            log.warn("[STUB] Unknown player: {}", playerId);
            return state;
        }
        Player opponent = state.getOpponent(playerId);

        switch (actionType) {
            case "SUMMON" -> summon(player, action, CardPosition.ATTACK);
            case "SET" -> setCard(player, action);
            case "SPELL" -> playSpell(player, action);
            case "ATTACK" -> attack(state, player, opponent, action);
            default -> log.warn("[STUB] Unsupported action type: {}", action.getActionType());
        }

        updateGameStatus(state);
        return state;
    }

    @Override
    public DuelState advancePhase(DuelState state) {
        Phase current = state.getCurrentPhase();
        Phase next = switch (state.getCurrentPhase()) {
            case DRAW    -> Phase.STANDBY;
            case STANDBY -> Phase.MAIN_1;
            case MAIN_1  -> Phase.BATTLE;
            case BATTLE  -> Phase.MAIN_2;
            case MAIN_2  -> Phase.END;
            case END     -> Phase.DRAW;
        };
        log.info("[STUB] advancePhase: {} → {}", state.getCurrentPhase(), next);

        if (current == Phase.END) {
            state.setTurnNumber(state.getTurnNumber() + 1);
            state.setActivePlayerId(state.getOpponent(state.getActivePlayerId()).getPlayerId());
            state.setFirstTurn(false);
            drawCard(state.getActivePlayer());
        }

        state.setCurrentPhase(next);
        return state;
    }

    @Override
    public boolean isActionValid(DuelState state, DuelActionDTO action, String playerId) {
        String actionType = action.getActionType() != null ? action.getActionType().toUpperCase() : "";
        boolean activePlayer = playerId != null && playerId.equals(state.getActivePlayerId());
        boolean inMain = state.getCurrentPhase() == Phase.MAIN_1 || state.getCurrentPhase() == Phase.MAIN_2;
        boolean inBattle = state.getCurrentPhase() == Phase.BATTLE;
        Player player = getPlayer(state, playerId);
        if (player == null) {
            return false;
        }

        boolean valid = switch (actionType) {
            case "SUMMON" -> activePlayer && inMain && findInHand(player, action.getCardId()) != null
                    && hasAvailableTarget(player.getMonsterZones(), action.getZoneIndex());
            case "SET" -> activePlayer && inMain && isSetActionValid(player, action);
            case "SPELL" -> activePlayer && inMain && findInHand(player, action.getCardId()) != null
                    && hasAvailableTarget(player.getSpellTrapZones(), action.getZoneIndex());
            case "ATTACK" -> activePlayer && inBattle && findAttacker(player, action.getCardId()) != null;
            default -> false;
        };

        log.info("[STUB] isActionValid: {} -> {}", action.getActionType(), valid);
        return valid;
    }

    private void summon(Player player, DuelActionDTO action, CardPosition position) {
        Zone zone = targetZone(player.getMonsterZones(), action.getZoneIndex());
        if (zone == null) {
            return;
        }

        Card card = removeFromHand(player, action.getCardId());
        if (card == null) {
            return;
        }

        zone.setCard(card);
        zone.setPosition(position);
    }

    private void setCard(Player player, DuelActionDTO action) {
        Card card = findInHand(player, action.getCardId());
        if (card == null) {
            return;
        }

        if (card.getType() == CardType.MONSTER) {
            summon(player, action, CardPosition.DEFENSE_FACE_DOWN);
            return;
        }

        playSpell(player, action);
    }

    private void playSpell(Player player, DuelActionDTO action) {
        Zone zone = targetZone(player.getSpellTrapZones(), action.getZoneIndex());
        if (zone == null) {
            return;
        }

        Card card = removeFromHand(player, action.getCardId());
        if (card == null) {
            return;
        }

        zone.setCard(card);
    }

    private void attack(DuelState state, Player player, Player opponent, DuelActionDTO action) {
        Zone attackerZone = findAttacker(player, action.getCardId());
        if (attackerZone == null || attackerZone.getCard() == null) {
            return;
        }

        Zone targetZone = findCardZone(opponent.getMonsterZones(), action.getTargetId());
        Card attacker = attackerZone.getCard();

        if (targetZone == null || targetZone.getCard() == null) {
            opponent.takeDamage(Math.max(0, attacker.getAtk()));
            return;
        }

        Card target = targetZone.getCard();
        int targetStat = targetZone.getPosition() == CardPosition.DEFENSE_FACE_UP || targetZone.getPosition() == CardPosition.DEFENSE_FACE_DOWN
                ? target.getDef()
                : target.getAtk();
        int diff = attacker.getAtk() - targetStat;

        if (diff > 0) {
            opponent.getGraveyard().add(target);
            targetZone.setCard(null);
            if (targetZone.getPosition() == CardPosition.ATTACK) {
                opponent.takeDamage(diff);
            }
        } else if (diff < 0) {
            if (targetZone.getPosition() == CardPosition.ATTACK) {
                player.getGraveyard().add(attacker);
                attackerZone.setCard(null);
            }
            player.takeDamage(Math.abs(diff));
        } else if (targetZone.getPosition() == CardPosition.ATTACK) {
            player.getGraveyard().add(attacker);
            opponent.getGraveyard().add(target);
            attackerZone.setCard(null);
            targetZone.setCard(null);
        }
    }

    private Player getPlayer(DuelState state, String playerId) {
        if (playerId == null) {
            return null;
        }
        if (state.getPlayerA().getPlayerId().equals(playerId)) {
            return state.getPlayerA();
        }
        if (state.getPlayerB().getPlayerId().equals(playerId)) {
            return state.getPlayerB();
        }
        return null;
    }

    private Card findInHand(Player player, String cardId) {
        return player.getHand().stream()
                .filter(card -> card.getCardId().equals(cardId))
                .findFirst()
                .orElse(null);
    }

    private Card removeFromHand(Player player, String cardId) {
        Card card = findInHand(player, cardId);
        if (card != null) {
            player.getHand().remove(card);
        }
        return card;
    }

    private Zone targetZone(java.util.List<Zone> zones, Integer preferredIndex) {
        if (preferredIndex != null) {
            return zones.stream()
                    .filter(zone -> zone.getIndex() == preferredIndex && zone.isEmpty())
                    .findFirst()
                    .orElse(null);
        }

        return zones.stream()
                .filter(Zone::isEmpty)
                .findFirst()
                .orElse(null);
    }

    private boolean hasFreeZone(java.util.List<Zone> zones) {
        return zones.stream().anyMatch(Zone::isEmpty);
    }

    private boolean hasAvailableTarget(java.util.List<Zone> zones, Integer preferredIndex) {
        return preferredIndex == null
                ? hasFreeZone(zones)
                : zones.stream().anyMatch(zone -> zone.getIndex() == preferredIndex && zone.isEmpty());
    }

    private boolean isSetActionValid(Player player, DuelActionDTO action) {
        Card card = findInHand(player, action.getCardId());
        if (card == null) {
            return false;
        }

        java.util.List<Zone> zones = card.getType() == CardType.MONSTER
                ? player.getMonsterZones()
                : player.getSpellTrapZones();

        return hasAvailableTarget(zones, action.getZoneIndex());
    }

    private Zone findAttacker(Player player, String cardId) {
        if (cardId != null) {
            return findCardZone(player.getMonsterZones(), cardId);
        }

        return player.getMonsterZones().stream()
                .filter(zone -> zone.getCard() != null)
                .findFirst()
                .orElse(null);
    }

    private Zone findCardZone(java.util.List<Zone> zones, String cardId) {
        if (cardId == null) {
            return null;
        }

        return zones.stream()
                .filter(zone -> zone.getCard() != null)
                .filter(zone -> cardId.equals(zone.getCard().getCardId()))
                .findFirst()
                .orElse(null);
    }

    private void drawCard(Player player) {
        if (!player.getDeck().isEmpty()) {
            player.getHand().add(player.getDeck().remove(0));
        }
    }

    private void updateGameStatus(DuelState state) {
        if (!state.getPlayerA().isAlive() || !state.getPlayerB().isAlive()) {
            state.setStatus(GameStatus.FINISHED);
            if (state.getPlayerA().isAlive() && !state.getPlayerB().isAlive()) {
                state.setWinnerId(state.getPlayerA().getPlayerId());
            } else if (!state.getPlayerA().isAlive() && state.getPlayerB().isAlive()) {
                state.setWinnerId(state.getPlayerB().getPlayerId());
            } else {
                log.info("[STUB] DRAW - both players defeated");
            }
        }
    }
}
