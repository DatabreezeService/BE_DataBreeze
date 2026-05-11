package databreeze.dto.etl;

import databreeze.entity.enums.DataSourceType;
import databreeze.entity.enums.SourcePlatform;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Metadata đi kèm file upload. Vì file dùng multipart/form-data nên Controller nhận các field này bằng @RequestParam.
 */
@Data
public class UploadFileRequest {
    @NotNull(message = "workspaceId là bắt buộc.")
    private UUID workspaceId;
    private UUID uploadedBy;
    private UUID storeId;
    private SourcePlatform platform = SourcePlatform.SHOPEE;
    private DataSourceType dataSourceType = DataSourceType.MARKETPLACE_ORDER;
}
