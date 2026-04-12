package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
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
        return state;
    }

    @Override
    public DuelState advancePhase(DuelState state) {
        Phase next = switch (state.getCurrentPhase()) {
            case DRAW    -> Phase.STANDBY;
            case STANDBY -> Phase.MAIN_1;
            case MAIN_1  -> Phase.BATTLE;
            case BATTLE  -> Phase.MAIN_2;
            case MAIN_2  -> Phase.END;
            case END     -> Phase.DRAW;
        };
        log.info("[STUB] advancePhase: {} → {}", state.getCurrentPhase(), next);
        state.setCurrentPhase(next);
        return state;
    }

    @Override
    public boolean isActionValid(DuelState state, DuelActionDTO action, String playerId) {
        log.info("[STUB] isActionValid: {} → true", action.getActionType());
        return true;
    }
}