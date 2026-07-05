package com.example.subscriptions.application.dto;

import com.example.subscriptions.domain.enums.CurrencyCode;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.domain.enums.RecurrenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateObligationRequest(
        @NotBlank String title,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull CurrencyCode currency,
        @NotNull ObligationCategory category,
        RecurrenceType recurrence,
        @NotNull LocalDate nextPaymentDate
) {
}
