package com.odevpedro.yugiohcollections.duel.application.service;

import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;

public interface PhaseService {
    DuelState advance(DuelState state);
    boolean isActionAllowed(Phase phase, String actionType);
}
