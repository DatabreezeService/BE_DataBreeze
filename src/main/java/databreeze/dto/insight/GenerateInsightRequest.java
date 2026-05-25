package databreeze.dto.insight;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateInsightRequest {
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;

    @AssertTrue(message = "fromDate phải nhỏ hơn hoặc bằng toDate.")
    public boolean isValidDateRange() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
