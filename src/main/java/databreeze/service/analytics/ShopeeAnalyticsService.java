package databreeze.service.analytics;

import databreeze.dto.shopee.ShopeeDailyCalculationResult;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service tính lại bảng tổng hợp dashboard Shopee.
 */
public interface ShopeeAnalyticsService {
    /** Tính revenue_daily và profit_daily trong khoảng ngày phát sinh từ file import. */
    ShopeeDailyCalculationResult recalculateDaily(UUID workspaceId, UUID storeId, LocalDate fromDate, LocalDate toDate);
}
