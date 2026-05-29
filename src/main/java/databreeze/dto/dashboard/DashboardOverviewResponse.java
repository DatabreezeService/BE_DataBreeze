package databreeze.dto.dashboard;

import databreeze.enums.SourcePlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewResponse {
    private UUID workspaceId;
    private UUID storeId;
    private SourcePlatform sourcePlatform;
    private LocalDate fromDate;
    private LocalDate toDate;
    private DashboardSummaryResponse summary;
    private List<DashboardDailyResponse> daily;
    private List<DashboardProductResponse> topProducts;
    private DashboardDataQualityResponse dataQuality;
}
