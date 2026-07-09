package com.odevpedro.yugiohcollections.duel.domain.exception;

import java.util.List;

public class InvalidDeckException extends RuntimeException {

    private final List<String> violations;

    public InvalidDeckException(List<String> violations) {
        super("Deck validation failed");
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
