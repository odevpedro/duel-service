package com.odevpedro.yugiohcollections.duel.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDuelRequest {

    @NotBlank
    private String playerAId;

    @NotBlank
    private String playerBId;

    private Long playerADeckId;

    private Long playerBDeckId;
}
