package databreeze.service.dashboard;

import databreeze.dto.dashboard.DashboardDailyResponse;
import databreeze.dto.dashboard.DashboardOverviewResponse;
import databreeze.dto.dashboard.DashboardProductResponse;
import databreeze.dto.dashboard.DashboardSummaryResponse;
import databreeze.dto.shopee.ShopeeDailyCalculationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DashboardQueryService {
    DashboardOverviewResponse getShopeeDashboard(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate, int topProductLimit);

    DashboardSummaryResponse getShopeeSummary(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate);

    List<DashboardDailyResponse> getShopeeDaily(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate);

    List<DashboardProductResponse> getShopeeTopProducts(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate, int limit);

    ShopeeDailyCalculationResult recalculateShopee(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate);
}
