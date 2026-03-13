package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PhaseServiceImpl implements PhaseService {

    private static final Set<String> MAIN_PHASE_ACTIONS  = Set.of("SUMMON", "SPELL", "SET");
    private static final Set<String> BATTLE_PHASE_ACTIONS = Set.of("ATTACK");

    @Override
    public DuelState advance(DuelState state) {
        Phase next = switch (state.getCurrentPhase()) {
            case DRAW    -> Phase.STANDBY;
            case STANDBY -> Phase.MAIN_1;
            case MAIN_1  -> Phase.BATTLE;
            case BATTLE  -> Phase.MAIN_2;
            case MAIN_2  -> Phase.END;
            case END     -> Phase.DRAW;
        };

        if (next == Phase.DRAW) {
            state.setTurnNumber(state.getTurnNumber() + 1);
            String nextActive = state.getActivePlayerId().equals(state.getPlayerA().getPlayerId())
                    ? state.getPlayerB().getPlayerId()
                    : state.getPlayerA().getPlayerId();
            state.setActivePlayerId(nextActive);
        }

        state.setCurrentPhase(next);
        return state;
    }

    @Override
    public boolean isActionAllowed(Phase phase, String actionType) {
        return switch (phase) {
            case MAIN_1, MAIN_2 -> MAIN_PHASE_ACTIONS.contains(actionType);
            case BATTLE         -> BATTLE_PHASE_ACTIONS.contains(actionType);
            default             -> false;
        };
    }
}
