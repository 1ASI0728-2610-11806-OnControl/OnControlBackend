package com.oncontrol.oncontrolbackend.payments.infrastructure;

import com.oncontrol.oncontrolbackend.payments.application.DemoPaymentMethodService;
import com.oncontrol.oncontrolbackend.payments.application.DemoPaymentMethodService.PaymentMethodView;
import com.oncontrol.oncontrolbackend.payments.domain.DemoPaymentMethod.Scenario;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.ProfileType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-methods/demo")
@ConditionalOnProperty(name = "app.payments.demo.enabled", havingValue = "true")
public class DemoPaymentMethodController {
    private final DemoPaymentMethodService service;

    @GetMapping
    public List<PaymentMethodView> list(@AuthenticationPrincipal Profile profile) {
        return service.list(patientId(profile));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodView add(@AuthenticationPrincipal Profile profile, @Valid @RequestBody AddMethod request) {
        return service.add(patientId(profile), request.scenario());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Profile profile, @PathVariable Long id) {
        service.remove(patientId(profile), id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleError(ResponseStatusException exception) {
        String message = exception.getReason() != null ? exception.getReason() : "Acción no permitida";
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
    }

    private Long patientId(Profile profile) {
        if (profile == null || !profile.isEnabled() || profile.getProfileType() != ProfileType.PATIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return profile.getId();
    }

    public record AddMethod(@NotNull Scenario scenario) {}
}
