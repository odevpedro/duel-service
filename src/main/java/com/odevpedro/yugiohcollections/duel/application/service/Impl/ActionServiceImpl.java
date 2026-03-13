package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.DuelActionDTO;
import com.odevpedro.yugiohcollections.duel.application.service.ActionService;
import com.odevpedro.yugiohcollections.duel.application.service.PhaseService;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final PhaseService phaseService;
    private final DuelRepositoryPort repository;

    @Override
    public DuelState process(DuelActionDTO action, String playerId) {
        DuelState state = repository.findById(action.getDuelId())
                .orElseThrow(() -> new RuntimeException("Duel not found: " + action.getDuelId()));

        if (!phaseService.isActionAllowed(state.getCurrentPhase(), action.getActionType())) {
            throw new RuntimeException("Action not allowed in phase: " + state.getCurrentPhase());
        }

        DuelState updated = switch (action.getActionType()) {
            case "SUMMON" -> summon(state, playerId, action.getCardId(), action.getZoneIndex());
            case "ATTACK" -> attack(state, playerId, action.getTargetId());
            case "SPELL"  -> activateSpell(state, playerId, action.getCardId());
            default -> throw new RuntimeException("Unknown action: " + action.getActionType());
        };

        updated.setUpdatedAt(LocalDateTime.now());
        return repository.save(updated);
    }

    @Override
    public DuelState summon(DuelState state, String playerId, String cardId, int zoneIndex) {
        // TODO: implement summon logic
        return state;
    }

    @Override
    public DuelState attack(DuelState state, String attackerId, String targetId) {
        // TODO: implement attack logic
        return state;
    }

    @Override
    public DuelState activateSpell(DuelState state, String playerId, String cardId) {
        // TODO: implement spell activation logic
        return state;
    }
}
