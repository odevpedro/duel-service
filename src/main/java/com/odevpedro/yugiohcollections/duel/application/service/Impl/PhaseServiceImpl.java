package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {

    private final OcgCorePort ocgCore;

    @Override
    public DuelState advance(DuelState state) {
        return ocgCore.advancePhase(state);
    }

    @Override
    public boolean isActionAllowed(Phase phase, String actionType) {
        // validação delegada ao ocgcore via isActionValid no ActionService
        // este método fica como fallback para checks rápidos sem estado
        return switch (phase) {
            case MAIN_1, MAIN_2 -> actionType.equals("SUMMON") || actionType.equals("SPELL") || actionType.equals("SET");
            case BATTLE         -> actionType.equals("ATTACK");
            default             -> false;
        };
    }
}
