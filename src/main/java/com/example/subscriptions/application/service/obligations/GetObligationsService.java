package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.dto.ObligationResponse;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.repository.ObligationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetObligationsService {

    private final ObligationRepository obligationRepository;
    private final ObligationMapper mapper;
    private final ObligationDomainSupportService obligationDomainSupportService;

    @Transactional(readOnly = true)
    public List<ObligationResponse> getAll(ObligationCategory category, ObligationStatus status) {
        obligationDomainSupportService.applyLazyExpiry();

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

        return obligations.stream()
                .map(mapper::toResponse)
                .toList();
    }
}