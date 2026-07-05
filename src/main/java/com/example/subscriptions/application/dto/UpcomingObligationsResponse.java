package com.example.subscriptions.application.dto;

import com.example.subscriptions.domain.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpcomingObligationsResponse(
        List<ObligationResponse> obligations,
        Map<String, BigDecimal> totals,
        List<RenewalAlertResponse> renewalAlerts
) {
    public record RenewalAlertResponse(
            UUID id,
            String title,
            LocalDate nextPaymentDate,
            BigDecimal amount,
            CurrencyCode currency
    ) {
    }
}
