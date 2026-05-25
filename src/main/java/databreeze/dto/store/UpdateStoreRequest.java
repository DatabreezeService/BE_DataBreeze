package databreeze.dto.store;

import databreeze.enums.CommercePlatform;
import databreeze.enums.WorkspaceStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreRequest {
    @Size(max = 255, message = "Tên shop/store tối đa 255 ký tự.")
    private String name;

    private CommercePlatform platform;

    @Size(max = 255, message = "Mã store bên ngoài tối đa 255 ký tự.")
    private String externalStoreId;

    private String countryCode;

    private String currencyCode;

    private WorkspaceStatus status;
}
