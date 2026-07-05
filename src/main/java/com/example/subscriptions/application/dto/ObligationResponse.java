package com.example.subscriptions.application.dto;

import com.example.subscriptions.domain.enums.CurrencyCode;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.domain.enums.RecurrenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObligationResponse(
        UUID id,
        String title,
        BigDecimal amount,
        CurrencyCode currency,
        ObligationCategory category,
        RecurrenceType recurrence,
        LocalDate nextPaymentDate,
        ObligationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
