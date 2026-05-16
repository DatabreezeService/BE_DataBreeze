package databreeze.dto.admin;

import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    @Size(max = 255, message = "Ten toi da 255 ky tu.")
    private String fullName;

    @Size(max = 500, message = "Avatar url toi da 500 ky tu.")
    private String avatarUrl;

    private Boolean emailVerified;

    private UserType userType;

    private SystemRole systemRole;

    private UserStatus status;
}
