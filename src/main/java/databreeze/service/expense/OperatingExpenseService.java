package databreeze.service.expense;

import databreeze.dto.expense.OperatingExpenseRequest;
import databreeze.dto.expense.OperatingExpenseResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OperatingExpenseService {
    List<OperatingExpenseResponse> listExpenses(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate);

    OperatingExpenseResponse createExpense(UUID workspaceId, UUID actorUserId, OperatingExpenseRequest request);

    OperatingExpenseResponse updateExpense(UUID workspaceId, UUID actorUserId, UUID expenseId, OperatingExpenseRequest request);

    void deleteExpense(UUID workspaceId, UUID actorUserId, UUID expenseId);
}
