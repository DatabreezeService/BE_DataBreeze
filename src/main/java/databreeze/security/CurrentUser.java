package databreeze.security;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static UUID requireUserId(UserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new SecurityException("Khong tim thay user dang dang nhap. Vui long gui Bearer token hop le.");
        }
        return principal.userId();
    }
}
