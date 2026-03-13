package com.odevpedro.yugiohcollections.duel.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DuelActionDTO {

    @NotBlank
    private String duelId;

    @NotBlank
    private String actionType;

    private String cardId;
    private Integer zoneIndex;
    private String targetId;
}
