package com.example.subscriptions.application.service.obligations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.dto.UpcomingObligationsResponse;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.enums.CurrencyCode;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.domain.enums.RecurrenceType;
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
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetUpcomingObligationsServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private ObligationDomainSupportService obligationDomainSupportService;

    private GetUpcomingObligationsService getUpcomingObligationsService;

    @BeforeEach
    void setUp() {
        ObligationMapper mapper = Mappers.getMapper(ObligationMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);

        getUpcomingObligationsService = new GetUpcomingObligationsService(
                obligationRepository,
                mapper,
                obligationDomainSupportService,
                clock
        );
    }

    @Test
    void shouldReturnUpcomingTotalsAndRenewalAlerts() {
        Obligation netflix = obligation(
                UUID.randomUUID(),
                "Netflix",
                new BigDecimal("9.99"),
                CurrencyCode.USD,
                ObligationCategory.SUBSCRIPTION,
                RecurrenceType.MONTHLY,
                LocalDate.of(2026, 7, 5)
        );

        Obligation insurance = obligation(
                UUID.randomUUID(),
                "Insurance",
                new BigDecimal("1000.00"),
                CurrencyCode.RUB,
                ObligationCategory.INSURANCE,
                null,
                LocalDate.of(2026, 7, 6)
        );

        when(obligationRepository.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                LocalDate.of(2026, 7, 4),
                LocalDate.of(2026, 7, 11)
        )).thenReturn(List.of(netflix, insurance));

        UpcomingObligationsResponse result = getUpcomingObligationsService.getUpcoming(7);

        assertThat(result.obligations()).hasSize(2);
        assertThat(result.totals()).containsEntry("USD", new BigDecimal("9.99"));
        assertThat(result.totals()).containsEntry("RUB", new BigDecimal("1000.00"));
        assertThat(result.renewalAlerts()).hasSize(1);
        assertThat(result.renewalAlerts().getFirst().title()).isEqualTo("Netflix");
    }

    private Obligation obligation(
            UUID id,
            String title,
            BigDecimal amount,
            CurrencyCode currency,
            ObligationCategory category,
            RecurrenceType recurrenceType,
            LocalDate nextPaymentDate
    ) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle(title);
        obligation.setAmount(amount);
        obligation.setCurrency(currency);
        obligation.setCategory(category);
        obligation.setRecurrence(recurrenceType);
        obligation.setNextPaymentDate(nextPaymentDate);
        obligation.setStatus(ObligationStatus.ACTIVE);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}