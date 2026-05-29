package databreeze.dto.expense;

import databreeze.enums.AllocationMethod;
import databreeze.enums.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingExpenseResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID storeId;
    private LocalDate expenseDate;
    private ExpenseCategory category;
    private String description;
    private BigDecimal amount;
    private String currencyCode;
    private AllocationMethod allocationMethod;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
