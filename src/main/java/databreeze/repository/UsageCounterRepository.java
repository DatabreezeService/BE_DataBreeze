package databreeze.repository;

import databreeze.entity.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {
    Optional<UsageCounter> findByWorkspaceIdAndPeriodStartAndPeriodEnd(UUID workspaceId, LocalDate periodStart, LocalDate periodEnd);
}
