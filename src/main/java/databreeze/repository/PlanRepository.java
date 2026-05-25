package databreeze.repository;

import databreeze.entity.Plan;
import databreeze.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(String code);

    List<Plan> findByStatusOrderByPriceAmountAsc(PlanStatus status);
}
