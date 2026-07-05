package com.example.subscriptions.application.service.obligations.support;

import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.repository.ObligationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObligationDomainSupportService {
    private final ObligationRepository obligationRepository;
    private final Clock clock;

    public Obligation findByIdOrThrow(UUID id) {
        return obligationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Обязательство не найдено"));
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


