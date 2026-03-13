package com.odevpedro.yugiohcollections.duel.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhaseChangeDTO {

    @NotBlank
    private String duelId;
}
