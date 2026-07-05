package com.example.subscriptions.application.dto;

public record PayObligationResponse(
        ObligationResponse obligation,
        PaymentResponse payment
) {
}
