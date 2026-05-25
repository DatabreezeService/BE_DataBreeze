package databreeze.dto.expense;

import databreeze.enums.AllocationMethod;
import databreeze.enums.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatingExpenseRequest {
    private UUID storeId;

    @NotNull
    private LocalDate expenseDate;

    private ExpenseCategory category = ExpenseCategory.OTHER;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal amount;

    private String currencyCode = "VND";

    private AllocationMethod allocationMethod = AllocationMethod.MANUAL;
}
