package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.dto.ObligationResponse;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationStatus;
import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.repository.ObligationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelObligationService {

    private final ObligationRepository obligationRepository;
    private final ObligationMapper mapper;
    private final ObligationDomainSupportService obligationDomainSupportService;

    @Transactional
    public ObligationResponse cancel(UUID id) {
        Obligation obligation = obligationDomainSupportService.findByIdOrThrow(id);

        if (obligation.getStatus() != ObligationStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Отменить можно только обязательство в статусе active"
            );
        }

        obligation.setStatus(ObligationStatus.CANCELLED);
        return mapper.toResponse(obligationRepository.save(obligation));
    }
}
