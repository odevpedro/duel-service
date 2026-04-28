package com.odevpedro.yugiohcollections.duel.adapter.in.rest;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.dto.history.DuelHistoryResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelHistoryMapper;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository.DuelHistoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
public class DuelController {

    private final DuelApplicationService duelService;
    private final DuelMapper mapper;
    private final DuelHistoryRepository historyRepository;
    private final DuelHistoryMapper historyMapper;

    @PostMapping
    public ResponseEntity<DuelResponse> createDuel(@Valid @RequestBody CreateDuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(duelService.createDuel(request));
    }

    @GetMapping("/{duelId}")
    public ResponseEntity<DuelResponse> getDuel(@PathVariable String duelId) {
        return ResponseEntity.ok(mapper.toResponse(duelService.findById(duelId)));
    }

    @GetMapping("/{duelId}/history")
    public ResponseEntity<DuelHistoryResponse> getDuelHistory(@PathVariable String duelId) {
        return historyRepository.findByDuelId(duelId)
                .map(entity -> ResponseEntity.ok(historyMapper.toResponse(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<DuelHistoryResponse>> getRecentHistory() {
        List<DuelHistoryResponse> history = historyRepository.findTop100ByOrderByFinishedAtDesc()
                .stream()
                .map(historyMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/player/{playerId}")
    public ResponseEntity<List<DuelHistoryResponse>> getPlayerHistory(@PathVariable String playerId) {
        List<DuelHistoryResponse> history = historyRepository
                .findByPlayerAIdOrPlayerBIdOrderByFinishedAtDesc(playerId, playerId)
                .stream()
                .map(historyMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }
}
