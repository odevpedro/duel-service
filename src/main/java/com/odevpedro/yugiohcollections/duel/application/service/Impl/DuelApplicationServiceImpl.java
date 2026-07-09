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
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckViewResponse;
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckCardSummaryDTO;
import com.odevpedro.yugiohcollections.duel.adapter.out.messaging.DuelLifecycleKafkaPublisher;
import com.odevpedro.yugiohcollections.duel.domain.exception.InvalidDeckException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelApplicationServiceImpl implements DuelApplicationService {

    private static final int INITIAL_LIFE_POINTS = 8000;
    private static final int INITIAL_HAND_SIZE = 0;
    private static final int DEMO_DECK_SIZE = 40;
    private static final String DEFAULT_DUEL_TYPE = "CASUAL";

    private final DuelRepositoryPort repository;
    private final DuelHistoryRepository historyRepository;
    private final DeckFeignClient deckFeignClient;
    private final DuelLifecycleKafkaPublisher lifecyclePublisher;
    private final DuelMapper mapper;
    private final DuelHistoryMapper historyMapper;
    private final MeterRegistry meterRegistry;

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
                .playerADeckId(request.getPlayerADeckId())
                .playerBDeckId(request.getPlayerBDeckId())
                .playerA(playerA)
                .playerB(playerB)
                .duelType(request.getDuelType() != null ? request.getDuelType() : DEFAULT_DUEL_TYPE)
                .currentPhase(Phase.DRAW)
                .turnNumber(1)
                .activePlayerId(request.getPlayerAId())
                .status(GameStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .firstTurn(true)
                .build();

        DuelState saved = repository.save(state);
        lifecyclePublisher.publishDuelStarted(saved);
        meterRegistry.counter("duel.created").increment();
        return mapper.toResponse(saved);
    }

    private Player initializePlayer(String playerId, Long deckId) {
        LoadedDeck loadedDeck = loadDeckFromService(deckId);
        List<Card> deck = loadedDeck.mainDeck();
        shuffleDeck(deck);

        return Player.builder()
                .playerId(playerId)
                .lifePoints(INITIAL_LIFE_POINTS)
                .deck(deck)
                .hand(new ArrayList<>())
                .extraDeck(loadedDeck.extraDeck())
                .sideDeck(loadedDeck.sideDeck())
                .banished(new ArrayList<>())
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

    private LoadedDeck loadDeckFromService(Long deckId) {
        if (deckId == null) {
            if (demoDeckEnabled) {
                log.warn("No deck provided, using demo deck");
                return createDemoDeck();
            }
            log.warn("No deck provided, using empty deck");
            return new LoadedDeck(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        try {
            DeckViewResponse deckView = deckFeignClient.getDeck(deckId);
            List<String> violations = new ArrayList<>();
            if (deckView.getValidationErrors() != null) {
                violations.addAll(deckView.getValidationErrors());
            }
            if (!deckView.isValid()) {
                throw new InvalidDeckException(resolveViolations(deckView, violations));
            }

            LoadedDeck loaded = new LoadedDeck(
                    expandCards(deckView.getMainDeckCards()),
                    expandCards(deckView.getExtraDeckCards()),
                    expandCards(deckView.getSideDeckCards())
            );
            log.info("Loaded {} main cards, {} extra cards and {} side cards from deck {}",
                    loaded.mainDeck().size(), loaded.extraDeck().size(), loaded.sideDeck().size(), deckId);
            return loaded;
        } catch (Exception e) {
            log.error("Failed to load deck from deck-service: {}", e.getMessage());
            if (e instanceof InvalidDeckException invalidDeckException) {
                throw invalidDeckException;
            }
            throw new RuntimeException("Failed to load deck from deck-service", e);
        }
    }

    private LoadedDeck createDemoDeck() {
        List<Card> cards = new ArrayList<>();

        for (int i = 1; i <= DEMO_DECK_SIZE; i++) {
            CardType type = i % 10 == 0
                    ? CardType.TRAP
                    : i % 5 == 0 ? CardType.SPELL : CardType.MONSTER;

            long cardCode = type == CardType.MONSTER ? 89631139L + i : 0L;
            cards.add(Card.builder()
                    .cardId("demo-" + i)
                    .name(type == CardType.MONSTER ? "Demo Monster " + i : "Demo " + type.name() + " " + i)
                    .imageUrl(null)
                    .atk(type == CardType.MONSTER ? 900 + (i * 40) : 0)
                    .def(type == CardType.MONSTER ? 800 + (i * 35) : 0)
                    .level(type == CardType.MONSTER ? Math.min(8, 3 + (i % 5)) : 0)
                    .type(type)
                    .code(cardCode)
                    .build());
        }

        return new LoadedDeck(cards, new ArrayList<>(), new ArrayList<>());
    }

    private List<String> resolveViolations(DeckViewResponse deckView, List<String> violations) {
        List<String> resolved = new ArrayList<>(violations);
        if (deckView.getMainDeckSize() < 40 || deckView.getMainDeckSize() > 60) {
            resolved.add("Main deck must have 40-60 cards (current: " + deckView.getMainDeckSize() + ")");
        }
        if (deckView.getExtraDeckSize() > 15) {
            resolved.add("Extra deck must have at most 15 cards (current: " + deckView.getExtraDeckSize() + ")");
        }
        if (deckView.getSideDeckSize() > 15) {
            resolved.add("Side deck must have at most 15 cards (current: " + deckView.getSideDeckSize() + ")");
        }
        return resolved;
    }

    private List<Card> expandCards(List<DeckCardSummaryDTO> cards) {
        List<Card> expanded = new ArrayList<>();
        if (cards == null) {
            return expanded;
        }
        for (DeckCardSummaryDTO cardData : cards) {
            int quantity = cardData.getQuantity() != null ? Math.max(1, cardData.getQuantity()) : 1;
            for (int i = 0; i < quantity; i++) {
                long cardCode = cardData.getCardId() != null ? cardData.getCardId() : 0L;
                expanded.add(Card.builder()
                        .cardId(String.valueOf(cardCode))
                        .name(cardData.getName())
                        .imageUrl(cardData.getImageUrl())
                        .atk(valueOrZero(cardData.getAtk()))
                        .def(valueOrZero(cardData.getDef()))
                        .level(valueOrZero(cardData.getLevel()))
                        .type(extractType(cardData.getType()))
                        .code(cardCode)
                        .build());
            }
        }
        return expanded;
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
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
        state.setVictoryType(winnerId != null ? "NORMAL" : "DRAW");
        state.setUpdatedAt(LocalDateTime.now());
        
        var historyEntity = historyMapper.toEntity(state, winnerId);
        historyRepository.save(historyEntity);
        
        repository.save(state);
        lifecyclePublisher.publishDuelFinished(state);
        meterRegistry.counter("duel.finished").increment();
    }

    private record LoadedDeck(List<Card> mainDeck, List<Card> extraDeck, List<Card> sideDeck) {}
}
