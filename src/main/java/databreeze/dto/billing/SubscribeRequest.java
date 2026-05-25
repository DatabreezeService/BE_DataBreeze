package databreeze.dto.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {
    @NotBlank(message = "Mã gói subscription không được để trống.")
    private String planCode;
}
