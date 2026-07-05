package com.example.subscriptions.application.dto;

public record CreateObligationResult(
        ObligationResponse obligation,
        String warning
) {
}
