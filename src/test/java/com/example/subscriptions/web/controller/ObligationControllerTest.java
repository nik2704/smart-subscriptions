package com.example.subscriptions.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.subscriptions.application.dto.CreateObligationResult;
import com.example.subscriptions.application.dto.ObligationResponse;
import com.example.subscriptions.application.dto.PayObligationResponse;
import com.example.subscriptions.application.dto.PaymentResponse;
import com.example.subscriptions.application.dto.UpcomingObligationsResponse;
import com.example.subscriptions.application.service.obligations.CancelObligationService;
import com.example.subscriptions.application.service.obligations.CreateObligationService;
import com.example.subscriptions.application.service.obligations.DeleteObligationService;
import com.example.subscriptions.application.service.obligations.GetObligationsService;
import com.example.subscriptions.application.service.obligations.GetUpcomingObligationsService;
import com.example.subscriptions.application.service.obligations.PayObligationService;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.model.RecurrenceType;
import com.example.subscriptions.web.advice.GlobalExceptionHandler;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ObligationController.class)
@Import(GlobalExceptionHandler.class)
class ObligationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateObligationService createObligationService;

    @MockitoBean
    private GetObligationsService getObligationsService;

    @MockitoBean
    private GetUpcomingObligationsService getUpcomingObligationsService;

    @MockitoBean
    private PayObligationService payObligationService;

    @MockitoBean
    private CancelObligationService cancelObligationService;

    @MockitoBean
    private DeleteObligationService deleteObligationService;

    @MockitoBean
    private ObligationEventsPublisher eventsPublisher;

    @Test
    void shouldReturnWarningFieldOnDuplicateCreate() throws Exception {
        ObligationResponse obligationResponse = obligationResponse(
                UUID.randomUUID(),
                "Netflix",
                new BigDecimal("999.00"),
                "RUB",
                LocalDate.of(2026, 7, 10),
                ObligationStatus.ACTIVE
        );

        when(createObligationService.create(any())).thenReturn(
                new CreateObligationResult(
                        obligationResponse,
                        "Активное обязательство с таким названием уже существует"
                )
        );

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
                .andExpect(jsonPath("$.obligation.title").value("Netflix"))
                .andExpect(jsonPath("$.obligation.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnObligationsList() throws Exception {
        UUID id = UUID.randomUUID();

        when(getObligationsService.getAll(ObligationCategory.SUBSCRIPTION, ObligationStatus.ACTIVE))
                .thenReturn(List.of(
                        obligationResponse(
                                id,
                                "Netflix",
                                new BigDecimal("999.00"),
                                "RUB",
                                LocalDate.of(2026, 7, 10),
                                ObligationStatus.ACTIVE
                        )
                ));

        mockMvc.perform(get("/obligations")
                        .param("category", "SUBSCRIPTION")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].title").value("Netflix"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturnUpcomingObligations() throws Exception {
        UUID id = UUID.randomUUID();

        UpcomingObligationsResponse response = new UpcomingObligationsResponse(
                List.of(
                        obligationResponse(
                                id,
                                "Netflix",
                                new BigDecimal("9.99"),
                                "USD",
                                LocalDate.of(2026, 7, 10),
                                ObligationStatus.ACTIVE
                        )
                ),
                Map.of("USD", new BigDecimal("9.99")),
                List.of(
                        new UpcomingObligationsResponse.RenewalAlertResponse(
                                id,
                                "Netflix",
                                LocalDate.of(2026, 7, 10),
                                new BigDecimal("9.99"),
                                "USD"
                        )
                )
        );

        when(getUpcomingObligationsService.getUpcoming(7)).thenReturn(response);

        mockMvc.perform(get("/obligations/upcoming")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligations[0].title").value("Netflix"))
                .andExpect(jsonPath("$.totals.USD").value(9.99))
                .andExpect(jsonPath("$.renewalAlerts[0].title").value("Netflix"));
    }

    @Test
    void shouldReturnUpdatedObligationAndPaymentOnPay() throws Exception {
        UUID obligationId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PayObligationResponse response = new PayObligationResponse(
                obligationResponse(
                        obligationId,
                        "Netflix",
                        new BigDecimal("9.99"),
                        "USD",
                        LocalDate.of(2026, 8, 10),
                        ObligationStatus.ACTIVE
                ),
                new PaymentResponse(
                        paymentId,
                        obligationId,
                        new BigDecimal("9.99"),
                        "USD",
                        Instant.parse("2026-07-05T10:00:00Z")
                )
        );

        when(payObligationService.pay(obligationId)).thenReturn(response);

        mockMvc.perform(post("/obligations/{id}/pay", obligationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.id").value(obligationId.toString()))
                .andExpect(jsonPath("$.payment.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value("2026-08-10"));
    }

    @Test
    void shouldCancelObligation() throws Exception {
        UUID obligationId = UUID.randomUUID();

        when(cancelObligationService.cancel(obligationId)).thenReturn(
                obligationResponse(
                        obligationId,
                        "Spotify",
                        new BigDecimal("12.99"),
                        "USD",
                        LocalDate.of(2026, 7, 18),
                        ObligationStatus.CANCELLED
                )
        );

        mockMvc.perform(patch("/obligations/{id}/cancel", obligationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(obligationId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldDeleteObligation() throws Exception {
        UUID obligationId = UUID.randomUUID();

        doNothing().when(deleteObligationService).delete(obligationId);

        mockMvc.perform(delete("/obligations/{id}", obligationId))
                .andExpect(status().isNoContent());
    }

    private ObligationResponse obligationResponse(
            UUID id,
            String title,
            BigDecimal amount,
            String currency,
            LocalDate nextPaymentDate,
            ObligationStatus status
    ) {
        return new ObligationResponse(
                id,
                title,
                amount,
                currency,
                ObligationCategory.SUBSCRIPTION,
                RecurrenceType.MONTHLY,
                nextPaymentDate,
                status,
                Instant.parse("2026-07-04T00:00:00Z"),
                Instant.parse("2026-07-04T00:00:00Z")
        );
    }
}