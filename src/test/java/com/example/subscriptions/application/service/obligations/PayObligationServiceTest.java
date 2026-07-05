package com.example.subscriptions.application.service.obligations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.dto.PayObligationResponse;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.date.DateCalculatorService;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.repository.PaymentRepository;
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
class PayObligationServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ObligationDomainSupportService obligationDomainSupportService;

    private PayObligationService payObligationService;

    @BeforeEach
    void setUp() {
        ObligationMapper mapper = Mappers.getMapper(ObligationMapper.class);
        DateCalculatorService dateCalculatorService = new DateCalculatorService();
        payObligationService = new PayObligationService(
                obligationRepository,
                paymentRepository,
                mapper,
                dateCalculatorService,
                obligationDomainSupportService
        );
    }

    @Test
    void shouldPayMonthlyWithoutDateDriftFor31st() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, RecurrenceType.MONTHLY, LocalDate.of(2025, 1, 31), ObligationStatus.ACTIVE);

        when(obligationDomainSupportService.findByIdOrThrow(id)).thenReturn(obligation);
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = payObligationService.pay(id);

        assertThat(response.obligation().nextPaymentDate()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(response.obligation().status()).isEqualTo(ObligationStatus.ACTIVE);
    }

    @Test
    void shouldPayQuarterly() {
        assertRecurringShift(RecurrenceType.QUARTERLY, LocalDate.of(2026, 7, 31), LocalDate.of(2026, 10, 31));
    }

    @Test
    void shouldPayYearly() {
        assertRecurringShift(RecurrenceType.YEARLY, LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28));
    }

    @Test
    void shouldPayOneTimeAndCancel() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, null, LocalDate.of(2026, 7, 5), ObligationStatus.ACTIVE);

        when(obligationDomainSupportService.findByIdOrThrow(id)).thenReturn(obligation);
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = payObligationService.pay(id);

        assertThat(response.obligation().status()).isEqualTo(ObligationStatus.CANCELLED);
    }

    @Test
    void shouldRejectPayForNonActiveStatus() {
        UUID id = UUID.randomUUID();
        when(obligationDomainSupportService.findByIdOrThrow(id))
                .thenReturn(obligation(id, null, LocalDate.now(), ObligationStatus.CANCELLED));

        assertThatThrownBy(() -> payObligationService.pay(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Оплатить можно только обязательство в статусе active");
    }

    private void assertRecurringShift(RecurrenceType recurrenceType, LocalDate source, LocalDate expected) {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, recurrenceType, source, ObligationStatus.ACTIVE);

        when(obligationDomainSupportService.findByIdOrThrow(id)).thenReturn(obligation);
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = payObligationService.pay(id);

        assertThat(response.obligation().nextPaymentDate()).isEqualTo(expected);
        assertThat(response.obligation().status()).isEqualTo(ObligationStatus.ACTIVE);
    }

    private Obligation obligation(UUID id, RecurrenceType recurrenceType, LocalDate nextPaymentDate, ObligationStatus status) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(ObligationCategory.SUBSCRIPTION);
        obligation.setRecurrence(recurrenceType);
        obligation.setNextPaymentDate(nextPaymentDate);
        obligation.setStatus(status);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}