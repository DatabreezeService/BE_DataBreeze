package databreeze.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecalculateDashboardRequest {
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
