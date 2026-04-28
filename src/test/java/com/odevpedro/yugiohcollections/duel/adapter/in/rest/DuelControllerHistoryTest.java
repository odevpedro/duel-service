package com.odevpedro.yugiohcollections.duel.adapter.in.rest;

import com.odevpedro.yugiohcollections.duel.application.dto.history.DuelHistoryResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.entity.DuelHistoryEntity;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DuelController.class)
class DuelControllerHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DuelApplicationService duelService;

    @MockBean
    private DuelHistoryRepository historyRepository;

    @MockBean
    private DuelMapper mapper;

    @MockBean
    private DuelHistoryMapper historyMapper;

    @Test
    void shouldReturnDuelHistory() throws Exception {
        DuelHistoryEntity entity = DuelHistoryEntity.builder()
                .duelId("duel-123")
                .playerAId("player-a")
                .playerBId("player-b")
                .winnerId("player-a")
                .loserId("player-b")
                .turnCount(5)
                .result("COMPLETED")
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .finishedAt(LocalDateTime.now())
                .durationSeconds(600L)
                .build();

        DuelHistoryResponse response = DuelHistoryResponse.builder()
                .duelId("duel-123")
                .playerAId("player-a")
                .playerBId("player-b")
                .winnerId("player-a")
                .loserId("player-b")
                .turnCount(5)
                .result("COMPLETED")
                .build();

        when(historyRepository.findByDuelId("duel-123")).thenReturn(Optional.of(entity));
        when(historyMapper.toResponse(entity)).thenReturn(response);

        mockMvc.perform(get("/api/duels/history/duel-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duelId").value("duel-123"))
                .andExpect(jsonPath("$.winnerId").value("player-a"));
    }

    @Test
    void shouldReturn404WhenHistoryNotFound() throws Exception {
        when(historyRepository.findByDuelId("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/duels/history/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnRecentHistory() throws Exception {
        DuelHistoryEntity entity = DuelHistoryEntity.builder()
                .duelId("duel-123")
                .playerAId("player-a")
                .playerBId("player-b")
                .winnerId("player-a")
                .result("COMPLETED")
                .finishedAt(LocalDateTime.now())
                .build();

        DuelHistoryResponse response = DuelHistoryResponse.builder()
                .duelId("duel-123")
                .winnerId("player-a")
                .result("COMPLETED")
                .build();

        when(historyRepository.findTop100ByOrderByFinishedAtDesc()).thenReturn(List.of(entity));
        when(historyMapper.toResponse(entity)).thenReturn(response);

        mockMvc.perform(get("/api/duels/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].duelId").value("duel-123"));
    }
}