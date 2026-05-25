package databreeze.dto.dashboard;

import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecalculateAnalyticsResponse {
    private boolean success;
    private ShopeeDailyCalculationResult result;
    private String message;
}
