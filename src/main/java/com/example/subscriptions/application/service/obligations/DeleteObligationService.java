package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteObligationService {
    private final ObligationRepository obligationRepository;
    private final ObligationEventsPublisher eventsPublisher;
    private final ObligationDomainSupportService obligationDomainSupportService;

    @Transactional
    public void delete(UUID id) {
        Obligation obligation = obligationDomainSupportService.findByIdOrThrow(id);

        obligationRepository.delete(obligation);
        eventsPublisher.publishDeleted(id.toString());
    }
}
