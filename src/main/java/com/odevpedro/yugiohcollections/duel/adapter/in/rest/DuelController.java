package com.odevpedro.yugiohcollections.duel.adapter.in.rest;

import com.odevpedro.yugiohcollections.duel.application.dto.CreateDuelRequest;
import com.odevpedro.yugiohcollections.duel.application.dto.DuelResponse;
import com.odevpedro.yugiohcollections.duel.application.mapper.DuelMapper;
import com.odevpedro.yugiohcollections.duel.application.service.DuelApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
public class DuelController {

    private final DuelApplicationService duelService;
    private final DuelMapper mapper;

    @PostMapping
    public ResponseEntity<DuelResponse> createDuel(@Valid @RequestBody CreateDuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(duelService.createDuel(request));
    }

    @GetMapping("/{duelId}")
    public ResponseEntity<DuelResponse> getDuel(@PathVariable String duelId) {
        return ResponseEntity.ok(mapper.toResponse(duelService.findById(duelId)));
    }
}
