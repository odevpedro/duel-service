package com.odevpedro.yugiohcollections.duel.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "deck-service", url = "${deck-service.url:http://localhost:8081}")
public interface DeckFeignClient {

    @GetMapping("/decks/{deckId}/full")
    Map<String, Object> getDeck(@PathVariable Long deckId);
}
