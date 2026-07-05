package com.example.subscriptions.application.service.obligations.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.subscriptions.domain.enums.CurrencyCode;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.repository.ObligationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObligationDomainSupportServiceTest {

    @Mock
    private ObligationRepository obligationRepository;

    private ObligationDomainSupportService supportService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);
        supportService = new ObligationDomainSupportService(obligationRepository, clock);
    }

    @Test
    void shouldApplyLazyExpiryOnlyForNonRecurringActiveObligations() {
        Obligation oneTimeExpired = obligation(UUID.randomUUID(), LocalDate.of(2026, 7, 1));

        when(obligationRepository.findByStatusAndRecurrenceIsNullAndNextPaymentDateBefore(any(), any()))
                .thenReturn(List.of(oneTimeExpired));

        when(obligationRepository.saveAll(any())).
                thenAnswer(invocation -> invocation.getArgument(0));

        supportService.applyLazyExpiry();

        assertThat(oneTimeExpired.getStatus()).isEqualTo(ObligationStatus.EXPIRED);
    }

    private Obligation obligation(UUID id, LocalDate nextPaymentDate) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency(CurrencyCode.RUB);
        obligation.setCategory(ObligationCategory.SUBSCRIPTION);
        obligation.setRecurrence(null);
        obligation.setNextPaymentDate(nextPaymentDate);
        obligation.setStatus(ObligationStatus.ACTIVE);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}