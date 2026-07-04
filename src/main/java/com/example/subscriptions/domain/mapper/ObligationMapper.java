package com.example.subscriptions.domain.mapper;

import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.domain.dto.PaymentResponse;
import com.example.subscriptions.domain.dto.UpcomingObligationsResponse;
import org.springframework.stereotype.Component;

@Component
public class ObligationMapper {

    public ObligationResponse toResponse(Obligation obligation) {
        return new ObligationResponse(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getAmount(),
                obligation.getCurrency(),
                obligation.getCategory(),
                obligation.getRecurrence(),
                obligation.getNextPaymentDate(),
                obligation.getStatus(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt()
        );
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getObligation().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt()
        );
    }

    public UpcomingObligationsResponse.RenewalAlertResponse toRenewalAlert(Obligation obligation) {
        return new UpcomingObligationsResponse.RenewalAlertResponse(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getNextPaymentDate(),
                obligation.getAmount(),
                obligation.getCurrency()
        );
    }
}
