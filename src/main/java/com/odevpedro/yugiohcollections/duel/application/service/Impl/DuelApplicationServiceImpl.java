package com.odevpedro.yugiohcollections.duel.application.service.Impl;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.domain.model.Card;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.Player;
import com.odevpedro.yugiohcollections.duel.domain.model.Zone;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardPosition;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.CardType;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.Phase;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.ZoneType;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelApplicationServiceImpl implements DuelApplicationService {

    private static final int INITIAL_LIFE_POINTS = 8000;
    private static final int INITIAL_HAND_SIZE = 5;
    private static final int DEMO_DECK_SIZE = 40;

    private final DuelRepositoryPort repository;
    private final DuelHistoryRepository historyRepository;
    private final DeckFeignClient deckFeignClient;
    private final DuelMapper mapper;
    private final DuelHistoryMapper historyMapper;

    @Value("${duel.demo-deck.enabled:false}")
    private boolean demoDeckEnabled;

    @Override
    public DuelResponse createDuel(CreateDuelRequest request) {
        Player playerA = initializePlayer(request.getPlayerAId(), request.getPlayerADeckId());
        Player playerB = initializePlayer(request.getPlayerBId(), request.getPlayerBDeckId());

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
                .firstTurn(true)
                .build();

        return mapper.toResponse(repository.save(state));
    }

    private Player initializePlayer(String playerId, Long deckId) {
        List<Card> deck = loadDeckFromService(deckId);
        shuffleDeck(deck);
        List<Card> hand = drawCards(deck, INITIAL_HAND_SIZE);

        return Player.builder()
                .playerId(playerId)
                .lifePoints(INITIAL_LIFE_POINTS)
                .deck(deck)
                .hand(hand)
                .monsterZones(createZones(ZoneType.MONSTER))
                .spellTrapZones(createZones(ZoneType.SPELL_TRAP))
                .build();
    }

    private List<Zone> createZones(ZoneType type) {
        List<Zone> zones = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            zones.add(Zone.builder()
                    .index(i)
                    .type(type)
                    .position(CardPosition.ATTACK)
                    .build());
        }
        return zones;
    }

    private void shuffleDeck(List<Card> deck) {
        Collections.shuffle(deck, ThreadLocalRandom.current());
    }

    private List<Card> drawCards(List<Card> deck, int amount) {
        List<Card> drawn = new ArrayList<>();
        int cardsToDraw = Math.min(amount, deck.size());

        for (int i = 0; i < cardsToDraw; i++) {
            drawn.add(deck.remove(0));
        }

        return drawn;
    }

    private List<Card> loadDeckFromService(Long deckId) {
        if (deckId == null) {
            if (demoDeckEnabled) {
                log.warn("No deck provided, using demo deck");
                return createDemoDeck();
            }
            log.warn("No deck provided, using empty deck");
            return new ArrayList<>();
        }

        try {
            Map<String, Object> deckView = deckFeignClient.getDeck(deckId);
            List<Map<String, Object>> deckCards = extractCards(deckView);
            List<Card> cards = new ArrayList<>();
            
            for (Map<String, Object> cardData : deckCards) {
                int quantity = extractQuantity(cardData);
                for (int i = 0; i < quantity; i++) {
                    Card card = Card.builder()
                            .cardId(extractCardId(cardData))
                            .name((String) cardData.get("name"))
                            .atk(extractInt(cardData, 1000, "atk", "attack"))
                            .def(extractInt(cardData, 1000, "def", "defense"))
                            .level(extractInt(cardData, 4, "level"))
                            .type(extractType(cardData.get("type")))
                            .build();
                    cards.add(card);
                }
            }
            
            log.info("Loaded {} cards from deck {}", cards.size(), deckId);
            return cards;
        } catch (Exception e) {
            log.error("Failed to load deck from deck-service: {}", e.getMessage());
            return demoDeckEnabled ? createDemoDeck() : new ArrayList<>();
        }
    }

    private List<Card> createDemoDeck() {
        List<Card> cards = new ArrayList<>();

        for (int i = 1; i <= DEMO_DECK_SIZE; i++) {
            CardType type = i % 10 == 0
                    ? CardType.TRAP
                    : i % 5 == 0 ? CardType.SPELL : CardType.MONSTER;

            cards.add(Card.builder()
                    .cardId("demo-" + i)
                    .name(type == CardType.MONSTER ? "Demo Monster " + i : "Demo " + type.name() + " " + i)
                    .atk(type == CardType.MONSTER ? 900 + (i * 40) : 0)
                    .def(type == CardType.MONSTER ? 800 + (i * 35) : 0)
                    .level(type == CardType.MONSTER ? Math.min(8, 3 + (i % 5)) : 0)
                    .type(type)
                    .build());
        }

        return cards;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCards(Map<String, Object> deckView) {
        Object cards = deckView.get("cards");
        if (cards instanceof List<?> cardList) {
            return (List<Map<String, Object>>) cardList;
        }
        return List.of();
    }

    private String extractCardId(Map<String, Object> cardData) {
        Object cardId = cardData.get("cardId");
        if (cardId == null) {
            cardId = cardData.get("id");
        }
        return String.valueOf(cardId);
    }

    private int extractQuantity(Map<String, Object> cardData) {
        Object quantity = cardData.get("quantity");
        if (quantity instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return 1;
    }

    private int extractInt(Map<String, Object> cardData, int defaultValue, String... keys) {
        for (String key : keys) {
            Object value = cardData.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    // Keep checking fallback keys.
                }
            }
        }
        return defaultValue;
    }

    private CardType extractType(Object type) {
        if (type == null) {
            return CardType.MONSTER;
        }

        String normalized = String.valueOf(type).toUpperCase();
        if (normalized.contains("SPELL")) {
            return CardType.SPELL;
        }
        if (normalized.contains("TRAP")) {
            return CardType.TRAP;
        }
        return CardType.MONSTER;
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
        state.setWinnerId(winnerId);
        state.setUpdatedAt(LocalDateTime.now());
        
        var historyEntity = historyMapper.toEntity(state, winnerId);
        historyRepository.save(historyEntity);
        
        repository.save(state);
    }
}
