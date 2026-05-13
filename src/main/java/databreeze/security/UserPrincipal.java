package databreeze.security;

import databreeze.enums.SystemRole;

import java.util.UUID;

public record UserPrincipal(UUID userId, String email, SystemRole systemRole) {
}
