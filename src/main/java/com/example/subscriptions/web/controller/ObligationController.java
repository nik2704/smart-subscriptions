package com.example.subscriptions.web.controller;

import com.example.subscriptions.domain.model.ObligationCategory;
import com.example.subscriptions.domain.model.ObligationStatus;
import com.example.subscriptions.domain.dto.CreateObligationRequest;
import com.example.subscriptions.domain.dto.CreateObligationResult;
import com.example.subscriptions.domain.dto.ObligationResponse;
import com.example.subscriptions.domain.dto.PayObligationResponse;
import com.example.subscriptions.domain.dto.UpcomingObligationsResponse;
import com.example.subscriptions.domain.service.ObligationService;
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

    private final ObligationService obligationService;
    private final ObligationEventsPublisher eventsPublisher;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult create(@Valid @RequestBody CreateObligationRequest request) {
        return obligationService.create(request);
    }

    @GetMapping
    public List<ObligationResponse> getAll(
            @RequestParam(required = false) ObligationCategory category,
            @RequestParam(required = false) ObligationStatus status
    ) {
        return obligationService.getAll(category, status);
    }

    @GetMapping("/upcoming")
    public UpcomingObligationsResponse getUpcoming(@RequestParam(defaultValue = "7") int days) {
        return obligationService.getUpcoming(days);
    }

    @PostMapping("/{id}/pay")
    public PayObligationResponse pay(@PathVariable UUID id) {
        return obligationService.pay(id);
    }

    @PatchMapping("/{id}/cancel")
    public ObligationResponse cancel(@PathVariable UUID id) {
        return obligationService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        obligationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/events", produces = "text/event-stream")
    public SseEmitter events() {
        return eventsPublisher.subscribe();
    }
}
