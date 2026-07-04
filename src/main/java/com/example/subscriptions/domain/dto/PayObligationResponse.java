package com.example.subscriptions.domain.dto;

public record PayObligationResponse(
        ObligationResponse obligation,
        PaymentResponse payment
) {
}
