package databreeze.service.analytics;

import databreeze.dto.analytics.ProcessedCommerceDataResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface ProcessedDataQueryService {
    ProcessedCommerceDataResponse getShopeeProcessedData(
            UUID workspaceId,
            UUID actorUserId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
