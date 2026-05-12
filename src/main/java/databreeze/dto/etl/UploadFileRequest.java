package databreeze.dto.etl;

import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UploadFileRequest {
    @NotNull(message = "workspaceId là bắt buộc.")
    private UUID workspaceId;

    @NotNull(message = "actorUserId là bắt buộc.")
    private UUID actorUserId;

    private UUID storeId;
    private SourcePlatform platform = SourcePlatform.SHOPEE;
    private DataSourceType dataSourceType = DataSourceType.MARKETPLACE_ORDER;
}
