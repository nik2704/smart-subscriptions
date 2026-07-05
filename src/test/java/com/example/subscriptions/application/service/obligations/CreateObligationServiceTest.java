package com.example.subscriptions.application.service.obligations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.subscriptions.application.dto.CreateObligationRequest;
import com.example.subscriptions.application.dto.CreateObligationResult;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.repository.ObligationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateObligationServiceTest {

    @Mock
    private ObligationRepository obligationRepository;

    private CreateObligationService createObligationService;

    @BeforeEach
    void setUp() {
        ObligationMapper mapper = Mappers.getMapper(ObligationMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);

        createObligationService = new CreateObligationService(
                obligationRepository,
                mapper,
                clock
        );
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
                .thenReturn(
                        Optional.of(
                                obligation(
                                        UUID.randomUUID(),
                                        LocalDate.of(2026, 7, 10)
                                )
                        )
                );

        when(obligationRepository.save(any())).thenAnswer(invocation -> {
            Obligation saved = invocation.getArgument(0);
            saved.prePersist();
            return saved;
        });

        CreateObligationResult result = createObligationService.create(request);

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

        CreateObligationResult result = createObligationService.create(request);

        assertThat(result.obligation().status()).isEqualTo(ObligationStatus.EXPIRED);
    }

    private Obligation obligation(UUID id, LocalDate nextPaymentDate) {
        Obligation obligation = new Obligation();
        obligation.setId(id);
        obligation.setTitle("Test");
        obligation.setAmount(new BigDecimal("100.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(ObligationCategory.SUBSCRIPTION);
        obligation.setRecurrence(RecurrenceType.MONTHLY);
        obligation.setNextPaymentDate(nextPaymentDate);
        obligation.setStatus(ObligationStatus.ACTIVE);
        obligation.setCreatedAt(Instant.now());
        obligation.setUpdatedAt(Instant.now());
        return obligation;
    }
}