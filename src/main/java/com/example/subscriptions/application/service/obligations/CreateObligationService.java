package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.dto.CreateObligationRequest;
import com.example.subscriptions.application.dto.CreateObligationResult;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CreateObligationService {
    private final ObligationRepository obligationRepository;
    private final ObligationMapper mapper;
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
}
