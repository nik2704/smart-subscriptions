package com.example.subscriptions.application.service.date;

import com.example.subscriptions.domain.model.RecurrenceType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DateCalculatorService {

    public LocalDate shift(LocalDate date, RecurrenceType recurrenceType) {
        return switch (recurrenceType) {
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }
}
