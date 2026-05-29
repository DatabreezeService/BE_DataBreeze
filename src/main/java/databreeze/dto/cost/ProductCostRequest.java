package databreeze.dto.cost;

import databreeze.enums.CostType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCostRequest {
    @NotBlank
    private String sku;

    private CostType costType = CostType.COGS;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal unitCost;

    private String currencyCode = "VND";

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
