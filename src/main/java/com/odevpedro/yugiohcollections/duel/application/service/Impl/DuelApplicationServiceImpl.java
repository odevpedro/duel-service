package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.domain.model.Card;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelApplicationServiceImpl implements DuelApplicationService {

    private static final int INITIAL_LIFE_POINTS = 8000;

    private final DuelRepositoryPort repository;
    private final DuelHistoryRepository historyRepository;
    private final DeckFeignClient deckFeignClient;
    private final DuelMapper mapper;
    private final DuelHistoryMapper historyMapper;

    @Override
    public DuelResponse createDuel(CreateDuelRequest request) {
        Player playerA = Player.builder()
                .playerId(request.getPlayerAId())
                .lifePoints(INITIAL_LIFE_POINTS)
                .deck(loadDeckFromService(request.getPlayerADeckId()))
                .build();

        Player playerB = Player.builder()
                .playerId(request.getPlayerBId())
                .lifePoints(INITIAL_LIFE_POINTS)
                .deck(loadDeckFromService(request.getPlayerBDeckId()))
                .build();

        DuelState state = DuelState.builder()
                .duelId(UUID.randomUUID().toString())
                .playerAId(request.getPlayerAId())
                .playerBId(request.getPlayerBId())
                .playerA(playerA)
                .playerB(playerB)
                .currentPhase(Phase.DRAW)
                .turnNumber(1)
                .activePlayerId(request.getPlayerAId())
                .status(GameStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mapper.toResponse(repository.save(state));
    }

    private List<Card> loadDeckFromService(Long deckId) {
        if (deckId == null) {
            log.warn("No deck provided, using empty deck");
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> deckCards = deckFeignClient.getDeckCards(deckId);
            List<Card> cards = new ArrayList<>();
            
            for (Map<String, Object> cardData : deckCards) {
                Card card = Card.builder()
                        .cardId(String.valueOf(cardData.get("id")))
                        .name((String) cardData.get("name"))
                        .build();
                cards.add(card);
            }
            
            log.info("Loaded {} cards from deck {}", cards.size(), deckId);
            return cards;
        } catch (Exception e) {
            log.error("Failed to load deck from deck-service: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public DuelState findById(String duelId) {
        return repository.findById(duelId)
                .orElseThrow(() -> new RuntimeException("Duel not found: " + duelId));
    }

    @Override
    public void endDuel(String duelId, String winnerId) {
        DuelState state = findById(duelId);
        state.setStatus(GameStatus.FINISHED);
        state.setUpdatedAt(LocalDateTime.now());
        
        var historyEntity = historyMapper.toEntity(state, winnerId);
        historyRepository.save(historyEntity);
        
        repository.save(state);
    }
}
