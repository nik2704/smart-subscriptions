package com.example.subscriptions.application.service.obligations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.repository.ObligationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelObligationServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private ObligationDomainSupportService obligationDomainSupportService;

    private ObligationMapper mapper;
    private CancelObligationService cancelObligationService;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ObligationMapper.class);
        cancelObligationService = new CancelObligationService(
                obligationRepository,
                mapper,
                obligationDomainSupportService
        );
    }

    @Test
    void shouldCancelActiveObligation() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, ObligationStatus.ACTIVE);

        when(obligationDomainSupportService.findByIdOrThrow(id)).thenReturn(obligation);
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cancelObligationService.cancel(id);

        assertThat(response.status()).isEqualTo(ObligationStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancelForNonActiveStatus() {
        UUID id = UUID.randomUUID();
        when(obligationDomainSupportService.findByIdOrThrow(id))
                .thenReturn(obligation(id, ObligationStatus.EXPIRED));

        assertThatThrownBy(() -> cancelObligationService.cancel(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Отменить можно только обязательство в статусе active");
    }

    private Obligation obligation(UUID id, ObligationStatus status) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(ObligationCategory.SUBSCRIPTION);
        obligation.setNextPaymentDate(LocalDate.of(2026, 7, 10));
        obligation.setStatus(status);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}