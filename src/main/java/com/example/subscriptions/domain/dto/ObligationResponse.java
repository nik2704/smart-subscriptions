package com.example.subscriptions.domain.dto;

import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.RecurrenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObligationResponse(
        UUID id,
        String title,
        BigDecimal amount,
        String currency,
        ObligationCategory category,
        RecurrenceType recurrence,
        LocalDate nextPaymentDate,
        ObligationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
