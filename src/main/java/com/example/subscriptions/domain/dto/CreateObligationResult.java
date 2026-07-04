package com.example.subscriptions.domain.dto;

public record CreateObligationResult(
        ObligationResponse obligation,
        String warning
) {
}
