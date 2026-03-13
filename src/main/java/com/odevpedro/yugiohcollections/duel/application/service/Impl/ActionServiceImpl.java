package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.application.service.ActionService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import com.odevpedro.yugiohcollections.duel.domain.port.OcgCorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final OcgCorePort ocgCore;
    private final DuelRepositoryPort repository;

    @Override
    public DuelState process(DuelActionDTO action, String playerId) {
        DuelState state = repository.findById(action.getDuelId())
                .orElseThrow(() -> new RuntimeException("Duel not found: " + action.getDuelId()));

        if (!ocgCore.isActionValid(state, action, playerId)) {
            throw new RuntimeException("Invalid action: " + action.getActionType()
                    + " in phase " + state.getCurrentPhase());
        }

        DuelState updated = ocgCore.processAction(state, action, playerId);
        updated.setUpdatedAt(LocalDateTime.now());

        return repository.save(updated);
    }

    @Override
    public DuelState summon(DuelState state, String playerId, String cardId, int zoneIndex) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId(state.getDuelId());
        action.setActionType("SUMMON");
        action.setCardId(cardId);
        action.setZoneIndex(zoneIndex);
        return ocgCore.processAction(state, action, playerId);
    }

    @Override
    public DuelState attack(DuelState state, String attackerId, String targetId) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId(state.getDuelId());
        action.setActionType("ATTACK");
        action.setTargetId(targetId);
        return ocgCore.processAction(state, action, attackerId);
    }

    @Override
    public DuelState activateSpell(DuelState state, String playerId, String cardId) {
        DuelActionDTO action = new DuelActionDTO();
        action.setDuelId(state.getDuelId());
        action.setActionType("SPELL");
        action.setCardId(cardId);
        return ocgCore.processAction(state, action, playerId);
    }
}
