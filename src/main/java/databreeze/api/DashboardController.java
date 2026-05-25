package databreeze.api;

import databreeze.dto.dashboard.DashboardDailyResponse;
import databreeze.dto.dashboard.DashboardOverviewResponse;
import databreeze.dto.dashboard.DashboardProductResponse;
import databreeze.dto.dashboard.RecalculateAnalyticsResponse;
import databreeze.dto.dashboard.RecalculateDashboardRequest;
import databreeze.dto.dashboard.DashboardSummaryResponse;
import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.dashboard.DashboardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/dashboard")
@Tag(name = "Dashboard Shopee Viet Nam", description = "API cho FE lay KPI, daily chart va top san pham sau import.")
@SecurityRequirement(name = "bearer")
public class DashboardController {

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @GetMapping("/shopee")
    @Operation(summary = "Lay dashboard Shopee gom summary, daily, top products va data quality")
    public DashboardOverviewResponse shopeeDashboard(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int topProductLimit
    ) {
        return dashboardQueryService.getShopeeDashboard(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                fromDate,
                toDate,
                topProductLimit
        );
    }

    @GetMapping("/shopee/summary")
    @Operation(summary = "Lay KPI tong quan Shopee")
    public DashboardSummaryResponse shopeeSummary(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return dashboardQueryService.getShopeeSummary(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                fromDate,
                toDate
        );
    }

    @GetMapping("/shopee/daily")
    @Operation(summary = "Lay daily chart doanh thu/loi nhuan Shopee")
    public List<DashboardDailyResponse> shopeeDaily(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return dashboardQueryService.getShopeeDaily(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                fromDate,
                toDate
        );
    }

    @GetMapping("/shopee/top-products")
    @Operation(summary = "Lay top san pham theo doanh thu Shopee")
    public List<DashboardProductResponse> shopeeTopProducts(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return dashboardQueryService.getShopeeTopProducts(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                fromDate,
                toDate,
                limit
        );
    }

    @PostMapping("/shopee/recalculate")
    @Operation(summary = "Tinh lai bang tong hop dashboard Shopee sau khi sua gia von/chi phi")
    public RecalculateAnalyticsResponse recalculateShopee(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RecalculateDashboardRequest request
    ) {
        RecalculateDashboardRequest safeRequest = request == null ? new RecalculateDashboardRequest() : request;
        ShopeeDailyCalculationResult result = dashboardQueryService.recalculateShopee(
                workspaceId,
                CurrentUser.requireUserId(principal),
                safeRequest.getStoreId(),
                safeRequest.getFromDate(),
                safeRequest.getToDate()
        );
        return RecalculateAnalyticsResponse.builder()
                .success(result.getRevenueDailyRows() > 0 || result.getProfitDailyRows() > 0)
                .result(result)
                .message(result.getMessage())
                .build();
    }
}
