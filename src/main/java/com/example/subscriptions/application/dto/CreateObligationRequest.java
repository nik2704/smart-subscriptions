package com.example.subscriptions.application.dto;

import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.RecurrenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateObligationRequest(
        @NotBlank String title,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull ObligationCategory category,
        RecurrenceType recurrence,
        @NotNull LocalDate nextPaymentDate
) {
}
