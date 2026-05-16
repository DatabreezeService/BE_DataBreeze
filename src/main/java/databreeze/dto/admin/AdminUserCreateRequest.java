package databreeze.dto.admin;

import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreateRequest {
    @NotBlank(message = "Email khong duoc de trong.")
    @Email(message = "Email khong hop le.")
    @Size(max = 255, message = "Email toi da 255 ky tu.")
    private String email;

    @Size(max = 255, message = "Ten toi da 255 ky tu.")
    private String fullName;

    @Size(max = 500, message = "Avatar url toi da 500 ky tu.")
    private String avatarUrl;

    @Size(max = 255, message = "Mat khau toi da 255 ky tu.")
    private String password;

    private AuthProvider authProvider = AuthProvider.EMAIL_PASSWORD;

    private Boolean emailVerified;

    private UserType userType = UserType.PERSONAL;

    private SystemRole systemRole = SystemRole.USER;

    private UserStatus status = UserStatus.ACTIVE;
}
