package com.example.subscriptions.domain.mapper;

import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.domain.dto.PaymentResponse;
import com.example.subscriptions.domain.dto.UpcomingObligationsResponse;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ObligationMapper {

    ObligationResponse toResponse(Obligation obligation);

    @Mapping(target = "obligationId", source = "obligation.id")
    PaymentResponse toResponse(Payment payment);

    UpcomingObligationsResponse.RenewalAlertResponse toRenewalAlert(Obligation obligation);
}