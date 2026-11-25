package com.oncontrol.oncontrolbackend.payments.application;

import com.oncontrol.oncontrolbackend.payments.domain.DemoPaymentMethod;
import com.oncontrol.oncontrolbackend.payments.domain.DemoPaymentMethod.Scenario;
import com.oncontrol.oncontrolbackend.payments.infrastructure.DemoPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payments.demo.enabled", havingValue = "true")
public class DemoPaymentMethodService {
    private final DemoPaymentMethodRepository repository;

    @Transactional(readOnly = true)
    public List<PaymentMethodView> list(Long profileId) {
        return repository.findByProfileIdOrderByIdDesc(profileId).stream().map(PaymentMethodView::from).toList();
    }

    @Transactional
    public PaymentMethodView add(Long profileId, Scenario scenario) {
        if (scenario == Scenario.DECLINED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El proveedor rechazó el método de prueba");
        }
        if (scenario == Scenario.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La tarjeta de prueba está vencida");
        }
        try {
            return PaymentMethodView.from(repository.saveAndFlush(new DemoPaymentMethod(profileId, scenario)));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este método de prueba ya está registrado", exception);
        }
    }

    @Transactional
    public void remove(Long profileId, Long id) {
        if (repository.deleteOwned(id, profileId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado");
        }
    }

    public record PaymentMethodView(Long id, String brand, String lastFour, Instant createdAt) {
        static PaymentMethodView from(DemoPaymentMethod method) {
            return new PaymentMethodView(method.getId(), method.getScenario().getBrand(),
                    method.getScenario().getLastFour(), method.getCreatedAt());
        }
    }
}
