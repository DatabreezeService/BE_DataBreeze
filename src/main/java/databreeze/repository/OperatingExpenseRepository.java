package databreeze.repository;

import databreeze.entity.OperatingExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperatingExpenseRepository extends JpaRepository<OperatingExpense, UUID> {
    List<OperatingExpense> findByWorkspaceIdOrderByExpenseDateDescCreatedAtDesc(UUID workspaceId);

    List<OperatingExpense> findByWorkspaceIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(UUID workspaceId, LocalDate fromDate, LocalDate toDate);

    Optional<OperatingExpense> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
