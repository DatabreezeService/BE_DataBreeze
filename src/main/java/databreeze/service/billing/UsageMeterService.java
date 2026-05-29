package databreeze.service.billing;

import databreeze.dto.billing.SubscriptionResponse;
import databreeze.dto.billing.UsageResponse;

import java.util.UUID;

public interface UsageMeterService {
    SubscriptionResponse getSubscription(UUID workspaceId, UUID actorUserId);

    UsageResponse getUsage(UUID workspaceId, UUID actorUserId);

    void recordUpload(UUID workspaceId, UUID actorUserId, long fileSizeBytes);

    void recordImportedRows(UUID workspaceId, UUID actorUserId, long importedRows);

    void ensureAiMappingAvailable(UUID workspaceId, UUID actorUserId);

    void recordAiMapping(UUID workspaceId, UUID actorUserId, long actualInputTokens, long actualOutputTokens);

    void recordInsightGeneration(UUID workspaceId, UUID actorUserId, long actualInputTokens, long actualOutputTokens);
}
