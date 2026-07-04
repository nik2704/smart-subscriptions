package com.example.subscriptions.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.domain.dto.CreateObligationRequest;
import com.example.subscriptions.domain.dto.CreateObligationResult;
import com.example.subscriptions.domain.dto.PayObligationResponse;

import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.domain.mapper.ObligationMapper;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.repository.PaymentRepository;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObligationServiceTest {

    @Mock
    private ObligationRepository obligationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ObligationEventsPublisher eventsPublisher;

    private ObligationMapper mapper;
    private DateCalculatorService dateCalculatorService;
    private Clock clock;
    private ObligationService obligationService;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ObligationMapper.class);
        dateCalculatorService = new DateCalculatorService();
        clock = Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);
        obligationService = new ObligationService(
                obligationRepository,
                paymentRepository,
                mapper,
                dateCalculatorService,
                eventsPublisher,
                clock
        );
    }

    @Test
    void shouldApplyLazyExpiryOnlyForNonRecurringActiveObligations() {
        Obligation oneTimeExpired = obligation(UUID.randomUUID(), RecurrenceType.MONTHLY, LocalDate.of(2026, 7, 1), ObligationStatus.ACTIVE);
        oneTimeExpired.setRecurrence(null);
        Obligation recurring = obligation(UUID.randomUUID(), RecurrenceType.MONTHLY, LocalDate.of(2026, 7, 1), ObligationStatus.ACTIVE);
        when(obligationRepository.findByStatusAndRecurrenceIsNullAndNextPaymentDateBefore(any(), any()))
                .thenReturn(List.of(oneTimeExpired));
        when(obligationRepository.findAllByOrderByNextPaymentDateAsc()).thenReturn(List.of(oneTimeExpired, recurring));
        when(obligationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = obligationService.getAll(null, null);

        assertThat(result).hasSize(2);
        assertThat(oneTimeExpired.getStatus()).isEqualTo(ObligationStatus.EXPIRED);
        assertThat(recurring.getStatus()).isEqualTo(ObligationStatus.ACTIVE);
    }

    @Test
    void shouldReturnWarningWhenDuplicateActiveTitleExists() {
        CreateObligationRequest request = new CreateObligationRequest(
                "Netflix",
                new BigDecimal("999.00"),
                "RUB",
                ObligationCategory.SUBSCRIPTION,
                RecurrenceType.MONTHLY,
                LocalDate.of(2026, 7, 10)
        );
        when(obligationRepository.findFirstByStatusAndTitleIgnoreCase(ObligationStatus.ACTIVE, "Netflix"))
                .thenReturn(Optional.of(obligation(UUID.randomUUID(), RecurrenceType.MONTHLY, LocalDate.of(2026, 7, 10), ObligationStatus.ACTIVE)));
        when(obligationRepository.save(any())).thenAnswer(invocation -> {
            Obligation saved = invocation.getArgument(0);
            saved.prePersist();
            return saved;
        });

        CreateObligationResult result = obligationService.create(request);

        assertThat(result.warning()).isEqualTo("Активное обязательство с таким названием уже существует");
        assertThat(result.obligation().status()).isEqualTo(ObligationStatus.ACTIVE);
    }

    @Test
    void shouldCreateExpiredWhenDateInPast() {
        CreateObligationRequest request = new CreateObligationRequest(
                "Старый чек",
                new BigDecimal("100.00"),
                "RUB",
                ObligationCategory.BILL,
                null,
                LocalDate.of(2026, 7, 1)
        );
        when(obligationRepository.findFirstByStatusAndTitleIgnoreCase(ObligationStatus.ACTIVE, "Старый чек"))
                .thenReturn(Optional.empty());
        when(obligationRepository.save(any())).thenAnswer(invocation -> {
            Obligation saved = invocation.getArgument(0);
            saved.prePersist();
            return saved;
        });

        CreateObligationResult result = obligationService.create(request);

        assertThat(result.obligation().status()).isEqualTo(ObligationStatus.EXPIRED);
    }

    @Test
    void shouldPayMonthlyWithoutDateDriftFor31st() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, RecurrenceType.MONTHLY, LocalDate.of(2025, 1, 31), ObligationStatus.ACTIVE);
        when(obligationRepository.findById(id)).thenReturn(Optional.of(obligation));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = obligationService.pay(id);

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
        when(obligationRepository.findById(id)).thenReturn(Optional.of(obligation));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = obligationService.pay(id);

        assertThat(response.obligation().status()).isEqualTo(ObligationStatus.CANCELLED);
    }

    @Test
    void shouldRejectPayForNonActiveStatus() {
        UUID id = UUID.randomUUID();
        when(obligationRepository.findById(id)).thenReturn(Optional.of(obligation(id, null, LocalDate.now(), ObligationStatus.CANCELLED)));

        assertThatThrownBy(() -> obligationService.pay(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Оплатить можно только обязательство в статусе active");
    }

    @Test
    void shouldRejectCancelForNonActiveStatus() {
        UUID id = UUID.randomUUID();
        when(obligationRepository.findById(id)).thenReturn(Optional.of(obligation(id, null, LocalDate.now(), ObligationStatus.EXPIRED)));

        assertThatThrownBy(() -> obligationService.cancel(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Отменить можно только обязательство в статусе active");
    }

    private void assertRecurringShift(RecurrenceType recurrenceType, LocalDate source, LocalDate expected) {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, recurrenceType, source, ObligationStatus.ACTIVE);
        when(obligationRepository.findById(id)).thenReturn(Optional.of(obligation));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.prePersist();
            return payment;
        });
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayObligationResponse response = obligationService.pay(id);

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
