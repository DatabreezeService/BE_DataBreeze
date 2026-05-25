package databreeze.service.expense.impl;

import databreeze.dto.expense.OperatingExpenseRequest;
import databreeze.dto.expense.OperatingExpenseResponse;
import databreeze.entity.OperatingExpense;
import databreeze.enums.AllocationMethod;
import databreeze.enums.ExpenseCategory;
import databreeze.enums.WorkspacePermission;
import databreeze.repository.OperatingExpenseRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
import databreeze.service.expense.OperatingExpenseService;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OperatingExpenseServiceImpl implements OperatingExpenseService {

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private OperatingExpenseRepository operatingExpenseRepository;

    @Autowired
    private ShopeeAnalyticsService shopeeAnalyticsService;

    @Override
    @Transactional(readOnly = true)
    public List<OperatingExpenseResponse> listExpenses(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.READ_FINANCIAL_DATA);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        LocalDate safeFrom = fromDate == null ? LocalDate.of(1900, 1, 1) : fromDate;
        LocalDate safeTo = toDate == null ? LocalDate.of(2999, 12, 31) : toDate;
        return operatingExpenseRepository.findByWorkspaceIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(workspaceId, safeFrom, safeTo)
                .stream()
                .filter(expense -> storeId == null || expense.getStoreId() == null || storeId.equals(expense.getStoreId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OperatingExpenseResponse createExpense(UUID workspaceId, UUID actorUserId, OperatingExpenseRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        OperatingExpense expense = new OperatingExpense();
        applyRequest(workspaceId, expense, request);
        OperatingExpense saved = operatingExpenseRepository.save(expense);
        recalculate(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OperatingExpenseResponse updateExpense(UUID workspaceId, UUID actorUserId, UUID expenseId, OperatingExpenseRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        OperatingExpense expense = requireExpense(workspaceId, expenseId);
        UUID oldStoreId = expense.getStoreId();
        LocalDate oldDate = expense.getExpenseDate();
        applyRequest(workspaceId, expense, request);
        OperatingExpense saved = operatingExpenseRepository.save(expense);
        shopeeAnalyticsService.recalculateDaily(workspaceId, oldStoreId, oldDate, oldDate);
        recalculate(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID workspaceId, UUID actorUserId, UUID expenseId) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        OperatingExpense expense = requireExpense(workspaceId, expenseId);
        UUID storeId = expense.getStoreId();
        LocalDate date = expense.getExpenseDate();
        operatingExpenseRepository.delete(expense);
        shopeeAnalyticsService.recalculateDaily(workspaceId, storeId, date, date);
    }

    private void applyRequest(UUID workspaceId, OperatingExpense expense, OperatingExpenseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu chi phi khong duoc de trong.");
        }
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, request.getStoreId());
        expense.setWorkspaceId(workspaceId);
        expense.setStoreId(request.getStoreId());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCategory(request.getCategory() == null ? ExpenseCategory.OTHER : request.getCategory());
        expense.setDescription(normalizeOptional(request.getDescription()));
        expense.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        expense.setCurrencyCode(request.getCurrencyCode() == null || request.getCurrencyCode().isBlank() ? "VND" : request.getCurrencyCode().trim().toUpperCase(Locale.ROOT));
        expense.setAllocationMethod(request.getAllocationMethod() == null ? AllocationMethod.MANUAL : request.getAllocationMethod());
    }

    private OperatingExpense requireExpense(UUID workspaceId, UUID expenseId) {
        return operatingExpenseRepository.findByIdAndWorkspaceId(expenseId, workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay chi phi van hanh."));
    }

    private void recalculate(OperatingExpense expense) {
        shopeeAnalyticsService.recalculateDaily(expense.getWorkspaceId(), expense.getStoreId(), expense.getExpenseDate(), expense.getExpenseDate());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private OperatingExpenseResponse toResponse(OperatingExpense expense) {
        return OperatingExpenseResponse.builder()
                .id(expense.getId())
                .workspaceId(expense.getWorkspaceId())
                .storeId(expense.getStoreId())
                .expenseDate(expense.getExpenseDate())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currencyCode(expense.getCurrencyCode())
                .allocationMethod(expense.getAllocationMethod())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
