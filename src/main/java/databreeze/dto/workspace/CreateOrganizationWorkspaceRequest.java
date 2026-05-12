package databreeze.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationWorkspaceRequest {
    @NotBlank(message = "Tên organization workspace không được để trống.")
    @Size(max = 255, message = "Tên organization workspace tối đa 255 ký tự.")
    private String workspaceName;

    @Size(max = 255, message = "Tên doanh nghiệp tối đa 255 ký tự.")
    private String businessName;

    @Size(max = 100, message = "Mã số thuế tối đa 100 ký tự.")
    private String taxCode;

    @Size(max = 255, message = "Email billing tối đa 255 ký tự.")
    private String billingEmail;
}
