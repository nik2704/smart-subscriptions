package com.example.subscriptions.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.domain.dto.CreateObligationResult;
import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.web.advice.GlobalExceptionHandler;
import com.example.subscriptions.domain.service.ObligationService;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ObligationController.class)
@Import(GlobalExceptionHandler.class)
class ObligationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObligationService obligationService;

    @MockitoBean
    private ObligationEventsPublisher eventsPublisher;

    @Test
    void shouldReturnWarningFieldOnDuplicateCreate() throws Exception {
        ObligationResponse obligationResponse = new ObligationResponse(
                UUID.randomUUID(),
                "Netflix",
                new BigDecimal("999.00"),
                "RUB",
                ObligationCategory.SUBSCRIPTION,
                RecurrenceType.MONTHLY,
                LocalDate.of(2026, 7, 10),
                ObligationStatus.ACTIVE,
                Instant.parse("2026-07-04T00:00:00Z"),
                Instant.parse("2026-07-04T00:00:00Z")
        );
        when(obligationService.create(any())).thenReturn(new CreateObligationResult(
                obligationResponse,
                "Активное обязательство с таким названием уже существует"
        ));

        mockMvc.perform(post("/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Netflix",
                                  "amount": 999.00,
                                  "currency": "RUB",
                                  "category": "SUBSCRIPTION",
                                  "recurrence": "MONTHLY",
                                  "nextPaymentDate": "2026-07-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning").value("Активное обязательство с таким названием уже существует"))
                .andExpect(jsonPath("$.obligation.title").value("Netflix"));
    }
}
