package com.odevpedro.yugiohcollections.duel.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "deck-service", url = "${deck-service.url:http://localhost:8082}")
public interface DeckFeignClient {

    @GetMapping("/api/decks/{deckId}/cards")
    List<Map<String, Object>> getDeckCards(@PathVariable Long deckId);
    
    @GetMapping("/api/decks/{deckId}")
    Map<String, Object> getDeck(@PathVariable Long deckId);
}