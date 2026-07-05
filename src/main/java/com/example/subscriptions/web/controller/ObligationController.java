package com.example.subscriptions.web.controller;

import com.example.subscriptions.application.service.obligations.*;
import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.application.dto.CreateObligationRequest;
import com.example.subscriptions.application.dto.CreateObligationResult;
import com.example.subscriptions.application.dto.ObligationResponse;
import com.example.subscriptions.application.dto.PayObligationResponse;
import com.example.subscriptions.application.dto.UpcomingObligationsResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.example.subscriptions.web.sse.ObligationEventsPublisher;

@RestController
@RequestMapping("/obligations")
@RequiredArgsConstructor
@Validated
public class ObligationController {

    private final CreateObligationService createObligationService;
    private final GetObligationsService getObligationsService;
    private final GetUpcomingObligationsService getUpcomingObligationsService;
    private final PayObligationService payObligationService;
    private final CancelObligationService cancelObligationService;
    private final DeleteObligationService deleteObligationService;
    private final ObligationEventsPublisher eventsPublisher;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult create(@Valid @RequestBody CreateObligationRequest request) {
        return createObligationService.create(request);
    }

    @GetMapping
    public List<ObligationResponse> getAll(
            @RequestParam(required = false) ObligationCategory category,
            @RequestParam(required = false) ObligationStatus status
    ) {
        return getObligationsService.getAll(category, status);
    }

    @GetMapping("/upcoming")
    public UpcomingObligationsResponse getUpcoming(@RequestParam(defaultValue = "7") int days) {
        return getUpcomingObligationsService.getUpcoming(days);
    }

    @PostMapping("/{id}/pay")
    public PayObligationResponse pay(@PathVariable UUID id) {
        return payObligationService.pay(id);
    }

    @PatchMapping("/{id}/cancel")
    public ObligationResponse cancel(@PathVariable UUID id) {
        return cancelObligationService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteObligationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/events", produces = "text/event-stream")
    public SseEmitter events() {
        return eventsPublisher.subscribe();
    }
}
