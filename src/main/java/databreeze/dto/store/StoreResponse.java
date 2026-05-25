package databreeze.dto.store;

import databreeze.enums.CommercePlatform;
import databreeze.enums.WorkspaceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private CommercePlatform platform;
    private String externalStoreId;
    private String countryCode;
    private String currencyCode;
    private WorkspaceStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
