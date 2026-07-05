package com.example.subscriptions.application.service.obligations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.enums.CurrencyCode;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteObligationServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private ObligationEventsPublisher eventsPublisher;
    @Mock
    private ObligationDomainSupportService obligationDomainSupportService;

    private DeleteObligationService deleteObligationService;

    @BeforeEach
    void setUp() {
        deleteObligationService = new DeleteObligationService(
                obligationRepository,
                eventsPublisher,
                obligationDomainSupportService
        );
    }

    @Test
    void shouldDeleteObligationAndPublishEvent() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id);

        when(obligationDomainSupportService.findByIdOrThrow(id)).thenReturn(obligation);

        deleteObligationService.delete(id);

        verify(obligationRepository).delete(obligation);
        verify(eventsPublisher).publishDeleted(id.toString());
    }

    private Obligation obligation(UUID id) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency(CurrencyCode.RUB);
        obligation.setCategory(ObligationCategory.BILL);
        obligation.setNextPaymentDate(LocalDate.of(2026, 7, 10));
        obligation.setStatus(ObligationStatus.ACTIVE);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}