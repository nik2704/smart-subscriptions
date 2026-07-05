package com.example.subscriptions.application.service.obligations;

import com.example.subscriptions.application.dto.UpcomingObligationsResponse;
import com.example.subscriptions.application.mapper.ObligationMapper;
import com.example.subscriptions.application.service.obligations.support.ObligationDomainSupportService;
import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.enums.ObligationCategory;
import com.example.subscriptions.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class GetUpcomingObligationsService {
    private final ObligationRepository obligationRepository;
    private final ObligationMapper mapper;
    private final ObligationDomainSupportService obligationDomainSupportService;
    private final Clock clock;

    @Transactional
    public UpcomingObligationsResponse getUpcoming(int days) {
        obligationDomainSupportService.applyLazyExpiry();
        LocalDate today = LocalDate.now(clock);
        LocalDate end = today.plusDays(days);
        List<Obligation> obligations = obligationRepository.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(today, end);

        Map<String, BigDecimal> totals = obligations.stream().collect(
                Collectors.groupingBy(
                        obligation -> obligation.getCurrency().name(),
                        Collectors.reducing(BigDecimal.ZERO, Obligation::getAmount, BigDecimal::add)
                )
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

}
