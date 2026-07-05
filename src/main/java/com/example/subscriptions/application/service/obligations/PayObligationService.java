package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.dto.PayObligationResponse;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.date.DateCalculatorService;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.exception.BusinessException;
import com.example.subscriptions.repository.ObligationRepository;
import com.example.subscriptions.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayObligationService {

    private final ObligationRepository obligationRepository;
    private final PaymentRepository paymentRepository;
    private final ObligationMapper mapper;
    private final DateCalculatorService dateCalculatorService;
    private final ObligationDomainSupportService obligationDomainSupportService;

    @Transactional
    public PayObligationResponse pay(UUID id) {
        Obligation obligation = obligationDomainSupportService.findByIdOrThrow(id);

        if (obligation.getStatus() != ObligationStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Оплатить можно только обязательство в статусе active"
            );
        }

        Payment payment = new Payment();
        payment.setObligation(obligation);
        payment.setAmount(obligation.getAmount());
        payment.setCurrency(obligation.getCurrency());
        Payment savedPayment = paymentRepository.save(payment);

        if (obligation.getRecurrence() == null) {
            obligation.setStatus(ObligationStatus.CANCELLED);
        } else {
            obligation.setNextPaymentDate(
                    dateCalculatorService.shift(
                            obligation.getNextPaymentDate(),
                            obligation.getRecurrence()
                    )
            );
            obligation.setStatus(ObligationStatus.ACTIVE);
        }

        Obligation savedObligation = obligationRepository.save(obligation);

        return new PayObligationResponse(
                mapper.toResponse(savedObligation),
                mapper.toResponse(savedPayment)
        );
    }
}