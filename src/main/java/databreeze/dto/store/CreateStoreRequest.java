package databreeze.dto.store;

import databreeze.enums.CommercePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreRequest {
    @NotBlank(message = "Tên shop/store không được để trống.")
    @Size(max = 255, message = "Tên shop/store tối đa 255 ký tự.")
    private String name;

    @NotNull(message = "Nền tảng shop là bắt buộc.")
    private CommercePlatform platform;

    @Size(max = 255, message = "Mã store bên ngoài tối đa 255 ký tự.")
    private String externalStoreId;

    @Builder.Default
    private String countryCode = "VN";

    @Builder.Default
    private String currencyCode = "VND";
}
