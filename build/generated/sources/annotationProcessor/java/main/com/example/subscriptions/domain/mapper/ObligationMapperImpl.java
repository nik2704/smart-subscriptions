package com.example.subscriptions.domain.mapper;

import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.domain.dto.PaymentResponse;
import com.example.subscriptions.domain.dto.UpcomingObligationsResponse;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.Payment;
import com.example.subscriptions.domain.model.RecurrenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-04T19:26:36+0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.2.jar, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ObligationMapperImpl implements ObligationMapper {

    @Override
    public ObligationResponse toResponse(Obligation obligation) {
        if ( obligation == null ) {
            return null;
        }

        UUID id = null;
        String title = null;
        BigDecimal amount = null;
        String currency = null;
        ObligationCategory category = null;
        RecurrenceType recurrence = null;
        LocalDate nextPaymentDate = null;
        ObligationStatus status = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = obligation.getId();
        title = obligation.getTitle();
        amount = obligation.getAmount();
        currency = obligation.getCurrency();
        category = obligation.getCategory();
        recurrence = obligation.getRecurrence();
        nextPaymentDate = obligation.getNextPaymentDate();
        status = obligation.getStatus();
        createdAt = obligation.getCreatedAt();
        updatedAt = obligation.getUpdatedAt();

        ObligationResponse obligationResponse = new ObligationResponse( id, title, amount, currency, category, recurrence, nextPaymentDate, status, createdAt, updatedAt );

        return obligationResponse;
    }

    @Override
    public PaymentResponse toResponse(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        UUID obligationId = null;
        UUID id = null;
        BigDecimal amount = null;
        String currency = null;
        Instant paidAt = null;

        obligationId = paymentObligationId( payment );
        id = payment.getId();
        amount = payment.getAmount();
        currency = payment.getCurrency();
        paidAt = payment.getPaidAt();

        PaymentResponse paymentResponse = new PaymentResponse( id, obligationId, amount, currency, paidAt );

        return paymentResponse;
    }

    @Override
    public UpcomingObligationsResponse.RenewalAlertResponse toRenewalAlert(Obligation obligation) {
        if ( obligation == null ) {
            return null;
        }

        UUID id = null;
        String title = null;
        LocalDate nextPaymentDate = null;
        BigDecimal amount = null;
        String currency = null;

        id = obligation.getId();
        title = obligation.getTitle();
        nextPaymentDate = obligation.getNextPaymentDate();
        amount = obligation.getAmount();
        currency = obligation.getCurrency();

        UpcomingObligationsResponse.RenewalAlertResponse renewalAlertResponse = new UpcomingObligationsResponse.RenewalAlertResponse( id, title, nextPaymentDate, amount, currency );

        return renewalAlertResponse;
    }

    private UUID paymentObligationId(Payment payment) {
        Obligation obligation = payment.getObligation();
        if ( obligation == null ) {
            return null;
        }
        return obligation.getId();
    }
}
