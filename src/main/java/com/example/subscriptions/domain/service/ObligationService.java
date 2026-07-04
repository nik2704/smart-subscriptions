package com.example.subscriptions.domain.service;

import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.domain.dto.CreateObligationRequest;
import com.example.subscriptions.domain.dto.CreateObligationResult;
import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.domain.dto.PayObligationResponse;
import com.example.subscriptions.domain.dto.UpcomingObligationsResponse;
import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.domain.mapper.ObligationMapper;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.repository.PaymentRepository;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ObligationService {

    private final ObligationRepository obligationRepository;
    private final PaymentRepository paymentRepository;
    private final ObligationMapper mapper;
    private final DateCalculatorService dateCalculatorService;
    private final ObligationEventsPublisher eventsPublisher;
    private final Clock clock;

    @Transactional
    public CreateObligationResult create(CreateObligationRequest request) {
        LocalDate today = LocalDate.now(clock);
        Obligation obligation = new Obligation();
        obligation.setTitle(request.title().trim());
        obligation.setAmount(request.amount());
        obligation.setCurrency(request.currency());
        obligation.setCategory(request.category());
        obligation.setRecurrence(request.recurrence());
        obligation.setNextPaymentDate(request.nextPaymentDate());
        obligation.setStatus(request.nextPaymentDate().isBefore(today) ? ObligationStatus.EXPIRED : ObligationStatus.ACTIVE);

        String warning = obligationRepository
                .findFirstByStatusAndTitleIgnoreCase(ObligationStatus.ACTIVE, request.title().trim())
                .map(existing -> "Активное обязательство с таким названием уже существует")
                .orElse(null);

        Obligation saved = obligationRepository.save(obligation);
        return new CreateObligationResult(mapper.toResponse(saved), warning);
    }

    @Transactional
    public List<ObligationResponse> getAll(ObligationCategory category, ObligationStatus status) {
        applyLazyExpiry();
        List<Obligation> obligations;
        if (category != null && status != null) {
            obligations = obligationRepository.findByCategoryAndStatusOrderByNextPaymentDateAsc(category, status);
        } else if (category != null) {
            obligations = obligationRepository.findByCategoryOrderByNextPaymentDateAsc(category);
        } else if (status != null) {
            obligations = obligationRepository.findByStatusOrderByNextPaymentDateAsc(status);
        } else {
            obligations = obligationRepository.findAllByOrderByNextPaymentDateAsc();
        }
        return obligations.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public UpcomingObligationsResponse getUpcoming(int days) {
        applyLazyExpiry();
        LocalDate today = LocalDate.now(clock);
        LocalDate end = today.plusDays(days);
        List<Obligation> obligations = obligationRepository.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(today, end);
        Map<String, BigDecimal> totals = obligations.stream().collect(
                Collectors.groupingBy(Obligation::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, Obligation::getAmount, BigDecimal::add))
        );
        List<UpcomingObligationsResponse.RenewalAlertResponse> alerts = obligations.stream()
                .filter(o -> o.getCategory() == ObligationCategory.SUBSCRIPTION && o.getRecurrence() != null)
                .map(mapper::toRenewalAlert)
                .toList();
        return new UpcomingObligationsResponse(
                obligations.stream().map(mapper::toResponse).toList(),
                totals,
                alerts
        );
    }

    @Transactional
    public PayObligationResponse pay(UUID id) {
        Obligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Обязательство не найдено"));
        if (obligation.getStatus() != ObligationStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Оплатить можно только обязательство в статусе active");
        }

        Payment payment = new Payment();
        payment.setObligation(obligation);
        payment.setAmount(obligation.getAmount());
        payment.setCurrency(obligation.getCurrency());
        Payment savedPayment = paymentRepository.save(payment);

        if (obligation.getRecurrence() == null) {
            obligation.setStatus(ObligationStatus.CANCELLED);
        } else {
            obligation.setNextPaymentDate(dateCalculatorService.shift(obligation.getNextPaymentDate(), obligation.getRecurrence()));
            obligation.setStatus(ObligationStatus.ACTIVE);
        }

        Obligation savedObligation = obligationRepository.save(obligation);
        return new PayObligationResponse(mapper.toResponse(savedObligation), mapper.toResponse(savedPayment));
    }

    @Transactional
    public ObligationResponse cancel(UUID id) {
        Obligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Обязательство не найдено"));
        if (obligation.getStatus() != ObligationStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Отменить можно только обязательство в статусе active");
        }
        obligation.setStatus(ObligationStatus.CANCELLED);
        return mapper.toResponse(obligationRepository.save(obligation));
    }

    @Transactional
    public void delete(UUID id) {
        Obligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Обязательство не найдено"));
        obligationRepository.delete(obligation);
        eventsPublisher.publishDeleted(id.toString());
    }

    @Transactional
    public void applyLazyExpiry() {
        LocalDate today = LocalDate.now(clock);
        List<Obligation> expired = obligationRepository.findByStatusAndRecurrenceIsNullAndNextPaymentDateBefore(
                ObligationStatus.ACTIVE,
                today
        );
        expired.forEach(item -> item.setStatus(ObligationStatus.EXPIRED));
        obligationRepository.saveAll(expired);
    }
}
