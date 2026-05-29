package databreeze.dto.cost;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkProductCostRequest {
    @NotEmpty
    private List<@Valid ProductCostRequest> costs;

    private boolean applyAfterSave = true;

    private UUID storeId;

    private LocalDate fromDate;

    private LocalDate toDate;
}
