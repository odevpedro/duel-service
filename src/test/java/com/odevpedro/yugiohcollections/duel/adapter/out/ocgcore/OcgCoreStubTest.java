package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.Card;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.Zone;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardType;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.ZoneType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OcgCoreStubTest {

    private final OcgCoreStub stub = new OcgCoreStub();

    @Test
    void shouldSummonMonsterFromHandToMonsterZone() {
        DuelState state = duelState(Phase.MAIN_1);
        DuelActionDTO action = action("SUMMON", "card-1");
        action.setZoneIndex(0);

        DuelState updated = stub.processAction(state, action, "player-a");

        assertThat(updated.getPlayerA().getHand()).isEmpty();
        assertThat(updated.getPlayerA().getMonsterZones().get(0).getCard().getCardId()).isEqualTo("card-1");
    }

    @Test
    void shouldSwitchActivePlayerIncrementTurnAndDrawWhenLeavingEndPhase() {
        DuelState state = duelState(Phase.END);
        int playerBHandSize = state.getPlayerB().getHand().size();
        int playerBDeckSize = state.getPlayerB().getDeck().size();

        DuelState updated = stub.advancePhase(state);

        assertThat(updated.getCurrentPhase()).isEqualTo(Phase.DRAW);
        assertThat(updated.getTurnNumber()).isEqualTo(2);
        assertThat(updated.getActivePlayerId()).isEqualTo("player-b");
        assertThat(updated.getPlayerB().getHand()).hasSize(playerBHandSize + 1);
        assertThat(updated.getPlayerB().getDeck()).hasSize(playerBDeckSize - 1);
        assertThat(updated.isFirstTurn()).isFalse();
    }

    @Test
    void shouldFinishDuelWhenDirectAttackDropsOpponentToZero() {
        DuelState state = duelState(Phase.BATTLE);
        state.getPlayerA().getHand().clear();
        state.getPlayerA().getMonsterZones().get(0).setCard(monster("attacker", 9000));

        DuelState updated = stub.processAction(state, action("ATTACK", "attacker"), "player-a");

        assertThat(updated.getPlayerB().getLifePoints()).isZero();
        assertThat(updated.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(updated.getWinnerId()).isEqualTo("player-a");
    }

    @Test
    void shouldRejectSpecificOccupiedZoneWithoutRemovingCardFromHand() {
        DuelState state = duelState(Phase.MAIN_1);
        state.getPlayerA().getMonsterZones().get(0).setCard(monster("field-card", 1000));
        DuelActionDTO action = action("SUMMON", "card-1");
        action.setZoneIndex(0);

        boolean valid = stub.isActionValid(state, action, "player-a");
        DuelState updated = stub.processAction(state, action, "player-a");

        assertThat(valid).isFalse();
        assertThat(updated.getPlayerA().getHand()).extracting(Card::getCardId).containsExactly("card-1");
        assertThat(updated.getPlayerA().getMonsterZones().get(0).getCard().getCardId()).isEqualTo("field-card");
    }

    private DuelState duelState(Phase phase) {
        return DuelState.builder()
                .duelId("duel-1")
                .playerAId("player-a")
                .playerBId("player-b")
                .playerA(player("player-a"))
                .playerB(player("player-b"))
                .activePlayerId("player-a")
                .currentPhase(phase)
                .turnNumber(1)
                .status(GameStatus.IN_PROGRESS)
                .firstTurn(true)
                .build();
    }

    private Player player(String playerId) {
        List<Card> hand = new ArrayList<>();
        hand.add(monster("card-1", 1000));

        List<Card> deck = new ArrayList<>();
        deck.add(monster("deck-1", 1000));

        return Player.builder()
                .playerId(playerId)
                .lifePoints(8000)
                .hand(hand)
                .deck(deck)
                .monsterZones(zones(ZoneType.MONSTER))
                .spellTrapZones(zones(ZoneType.SPELL_TRAP))
                .build();
    }

    private List<Zone> zones(ZoneType type) {
        List<Zone> zones = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            zones.add(Zone.builder().index(i).type(type).build());
        }
        return zones;
    }

    private Card monster(String cardId, int atk) {
        return Card.builder()
                .cardId(cardId)
                .name(cardId)
                .atk(atk)
                .def(1000)
                .level(4)
                .type(CardType.MONSTER)
                .build();
    }

    private DuelActionDTO action(String actionType, String cardId) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId("duel-1");
        action.setActionType(actionType);
        action.setCardId(cardId);
        return action;
    }
}
