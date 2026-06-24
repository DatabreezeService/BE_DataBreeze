package databreeze.dto.payments;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePaymentLinkRequestBody {
    private UUID workspaceId;

    private Long orderCode;

    @NotBlank(message = "productName khong duoc de trong")
    private String productName;

    @NotBlank(message = "description khong duoc de trong")
    private String description;

    @NotBlank(message = "returnUrl khong duoc de trong")
    private String returnUrl;

    @NotNull(message = "price khong duoc null")
    @Positive(message = "price phai lon hon 0")
    private Long price;

    @Min(value = 1, message = "quantity phai lon hon hoac bang 1")
    private Integer quantity = 1;

    @NotBlank(message = "cancelUrl khong duoc de trong")
    private String cancelUrl;
}
