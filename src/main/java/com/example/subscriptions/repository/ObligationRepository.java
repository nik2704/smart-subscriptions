package com.example.subscriptions.repository;

import com.example.subscriptions.domain.model.Obligation;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObligationRepository extends JpaRepository<Obligation, UUID> {

    Optional<Obligation> findFirstByStatusAndTitleIgnoreCase(ObligationStatus status, String title);

    List<Obligation> findByStatusAndRecurrenceIsNullAndNextPaymentDateBefore(
            ObligationStatus status,
            LocalDate date
    );

    List<Obligation> findAllByOrderByNextPaymentDateAsc();

    List<Obligation> findByCategoryAndStatusOrderByNextPaymentDateAsc(
            ObligationCategory category,
            ObligationStatus status
    );

    List<Obligation> findByCategoryOrderByNextPaymentDateAsc(ObligationCategory category);

    List<Obligation> findByStatusOrderByNextPaymentDateAsc(ObligationStatus status);

    List<Obligation> findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(LocalDate from, LocalDate to);
}
