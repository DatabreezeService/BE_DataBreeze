package databreeze.service.insight;

import databreeze.dto.insight.BusinessInsightResponse;
import databreeze.dto.insight.GenerateInsightRequest;
import databreeze.dto.insight.GenerateInsightResponse;
import databreeze.enums.InsightStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BusinessInsightService {
    GenerateInsightResponse generate(UUID workspaceId, UUID actorUserId, GenerateInsightRequest request);

    List<BusinessInsightResponse> list(UUID workspaceId, UUID actorUserId, InsightStatus status, UUID storeId, LocalDate fromDate, LocalDate toDate);

    BusinessInsightResponse updateStatus(UUID workspaceId, UUID actorUserId, UUID insightId, InsightStatus status);
}
