package databreeze.security;

import databreeze.enums.SystemRole;

public final class AdminAccess {
    private AdminAccess() {
    }

    public static void requireAdmin(UserPrincipal principal) {
        if (principal == null) {
            throw new SecurityException("Khong tim thay user dang dang nhap.");
        }
        SystemRole role = principal.systemRole();
        if (role != SystemRole.ADMIN && role != SystemRole.SUPER_ADMIN) {
            throw new SecurityException("Chi admin moi duoc phep thao tac.");
        }
    }
}
