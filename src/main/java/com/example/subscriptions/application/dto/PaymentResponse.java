package com.example.subscriptions.application.dto;

import com.example.subscriptions.domain.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID obligationId,
        BigDecimal amount,
        CurrencyCode currency,
        Instant paidAt
) {
}
