package com.oncontrol.oncontrolbackend.payments.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "demo_payment_methods", uniqueConstraints =
        @UniqueConstraint(name = "uk_demo_payment_profile_scenario", columnNames = {"profile_id", "scenario"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemoPaymentMethod {
    public enum Scenario {
        VALID_VISA("Visa", "4242"),
        VALID_MASTERCARD("Mastercard", "4444"),
        DECLINED(null, null),
        EXPIRED(null, null);

        @Getter
        private final String brand;
        @Getter
        private final String lastFour;

        Scenario(String brand, String lastFour) {
            this.brand = brand;
            this.lastFour = lastFour;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scenario scenario;

    @Column(nullable = false)
    private Instant createdAt;

    public DemoPaymentMethod(Long profileId, Scenario scenario) {
        this.profileId = profileId;
        this.scenario = scenario;
        this.createdAt = Instant.now();
    }
}
