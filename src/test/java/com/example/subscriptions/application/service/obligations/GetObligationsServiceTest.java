package com.example.subscriptions.application.service.obligations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.repository.ObligationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetObligationsServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private ObligationDomainSupportService obligationDomainSupportService;

    private GetObligationsService getObligationsService;

    @BeforeEach
    void setUp() {
        ObligationMapper mapper = Mappers.getMapper(ObligationMapper.class);
        getObligationsService = new GetObligationsService(
                obligationRepository,
                mapper,
                obligationDomainSupportService
        );
    }

    @Test
    void shouldReturnAllOrderedObligations() {
        Obligation first = obligation(UUID.randomUUID(), RecurrenceType.MONTHLY, LocalDate.of(2026, 7, 5));
        Obligation second = obligation(UUID.randomUUID(), null, LocalDate.of(2026, 7, 10));

        when(obligationRepository.findAllByOrderByNextPaymentDateAsc()).thenReturn(List.of(first, second));

        var result = getObligationsService.getAll(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nextPaymentDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(result.get(1).nextPaymentDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void shouldFilterByCategoryAndStatus() {
        Obligation item = obligation(UUID.randomUUID(), RecurrenceType.MONTHLY, LocalDate.of(2026, 7, 5));

        when(obligationRepository.findByCategoryAndStatusOrderByNextPaymentDateAsc(
                ObligationCategory.SUBSCRIPTION,
                ObligationStatus.ACTIVE
        )).thenReturn(List.of(item));

        var result = getObligationsService.getAll(ObligationCategory.SUBSCRIPTION, ObligationStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().category()).isEqualTo(ObligationCategory.SUBSCRIPTION);
        assertThat(result.getFirst().status()).isEqualTo(ObligationStatus.ACTIVE);
    }

    private Obligation obligation(UUID id, RecurrenceType recurrenceType, LocalDate nextPaymentDate) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(ObligationCategory.SUBSCRIPTION);
        obligation.setRecurrence(recurrenceType);
        obligation.setNextPaymentDate(nextPaymentDate);
        obligation.setStatus(ObligationStatus.ACTIVE);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}