package com.oncontrol.oncontrolbackend.payments.infrastructure;

import com.oncontrol.oncontrolbackend.payments.domain.DemoPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DemoPaymentMethodRepository extends JpaRepository<DemoPaymentMethod, Long> {
    List<DemoPaymentMethod> findByProfileIdOrderByIdDesc(Long profileId);

    @Modifying
    @Query("delete from DemoPaymentMethod m where m.id = :id and m.profileId = :profileId")
    int deleteOwned(Long id, Long profileId);
}
