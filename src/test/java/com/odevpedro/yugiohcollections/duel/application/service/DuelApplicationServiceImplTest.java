package com.odevpedro.yugiohcollections.duel.application.service;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.service.Impl.DuelApplicationServiceImpl;
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckFeignClient;
import com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckViewResponse;
import com.odevpedro.yugiohcollections.duel.adapter.out.messaging.DuelLifecycleKafkaPublisher;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.model.enums.GameStatus;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuelApplicationServiceImplTest {

    @Mock private DuelRepositoryPort repository;
    @Mock private DuelHistoryRepository historyRepository;
    @Mock private DeckFeignClient deckFeignClient;
    @Mock private DuelLifecycleKafkaPublisher lifecyclePublisher;
    @Mock private DuelMapper mapper;
    @Mock private DuelHistoryMapper historyMapper;
    @Mock private MeterRegistry meterRegistry;

    @InjectMocks
    private DuelApplicationServiceImpl service;

    private CreateDuelRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateDuelRequest();
        request.setPlayerAId("player-a");
        request.setPlayerBId("player-b");
    }

    @Test
    void shouldCreateDuelWithInitialLifePoints() {
        when(repository.save(any(DuelState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(DuelState.class))).thenAnswer(inv -> {
            DuelState s = inv.getArgument(0);
            return DuelResponse.builder()
                    .duelId(s.getDuelId())
                    .status(s.getStatus())
                    .build();
        });

        DuelResponse response = service.createDuel(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void shouldShuffleDeckAndDrawInitialHandsWhenCreatingDuel() {
        request.setPlayerADeckId(1L);
        request.setPlayerBDeckId(2L);

        when(deckFeignClient.getDeck(1L)).thenReturn(deckViewWithCards(1000));
        when(deckFeignClient.getDeck(2L)).thenReturn(deckViewWithCards(2000));
        when(repository.save(any(DuelState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(DuelState.class))).thenAnswer(inv -> {
            DuelState s = inv.getArgument(0);
            return DuelResponse.builder()
                    .duelId(s.getDuelId())
                    .status(s.getStatus())
                    .build();
        });

        service.createDuel(request);

        ArgumentCaptor<DuelState> stateCaptor = ArgumentCaptor.forClass(DuelState.class);
        org.mockito.Mockito.verify(repository).save(stateCaptor.capture());

        DuelState saved = stateCaptor.getValue();
        assertThat(saved.getPlayerA().getHand()).hasSize(5);
        assertThat(saved.getPlayerA().getDeck()).hasSize(35);
        assertThat(saved.getPlayerB().getHand()).hasSize(5);
        assertThat(saved.getPlayerB().getDeck()).hasSize(35);
        assertThat(saved.getTurnNumber()).isEqualTo(1);
    }

    @Test
    void shouldUseDemoDeckWhenNoDeckIsProvidedAndDemoDeckIsEnabled() {
        ReflectionTestUtils.setField(service, "demoDeckEnabled", true);
        when(repository.save(any(DuelState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(DuelState.class))).thenAnswer(inv -> {
            DuelState s = inv.getArgument(0);
            return DuelResponse.builder()
                    .duelId(s.getDuelId())
                    .status(s.getStatus())
                    .build();
        });

        service.createDuel(request);

        ArgumentCaptor<DuelState> stateCaptor = ArgumentCaptor.forClass(DuelState.class);
        org.mockito.Mockito.verify(repository).save(stateCaptor.capture());

        DuelState saved = stateCaptor.getValue();
        assertThat(saved.getPlayerA().getHand()).hasSize(5);
        assertThat(saved.getPlayerA().getDeck()).hasSize(35);
        assertThat(saved.getPlayerB().getHand()).hasSize(5);
        assertThat(saved.getPlayerB().getDeck()).hasSize(35);
    }

    @Test
    void shouldFindDuelById() {
        DuelState state = DuelState.builder()
                .duelId("duel-1")
                .playerA(com.odevpedro.yugiohcollections.duel.domain.model.Player.builder().playerId("a").lifePoints(8000).build())
                .playerB(com.odevpedro.yugiohcollections.duel.domain.model.Player.builder().playerId("b").lifePoints(8000).build())
                .build();
        when(repository.findById("duel-1")).thenReturn(Optional.of(state));

        DuelState found = service.findById("duel-1");

        assertThat(found.getDuelId()).isEqualTo("duel-1");
    }

    private DeckViewResponse deckViewWithCards(int startId) {
        DeckViewResponse response = new DeckViewResponse();
        response.setId((long) startId);
        response.setOwnerId("owner-" + startId);
        response.setName("Deck " + startId);
        response.setMainDeckCards(IntStream.range(0, 40)
                .mapToObj(index -> deckCard(startId + index))
                .toList());
        response.setExtraDeckCards(List.of());
        response.setSideDeckCards(List.of());
        response.setMainDeckSize(40);
        response.setExtraDeckSize(0);
        response.setSideDeckSize(0);
        response.setTotalCards(40);
        response.setValid(true);
        response.setValidationErrors(List.of());
        return response;
    }

    private com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckCardSummaryDTO deckCard(int cardId) {
        com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckCardSummaryDTO card =
                new com.odevpedro.yugiohcollections.duel.adapter.out.external.DeckCardSummaryDTO();
        card.setCardId((long) cardId);
        card.setName("Card " + cardId);
        card.setQuantity(1);
        card.setType("MONSTER");
        card.setAtk(1000);
        card.setDef(1000);
        card.setLevel(4);
        return card;
    }
}
