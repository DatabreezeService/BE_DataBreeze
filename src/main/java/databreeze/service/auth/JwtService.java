package databreeze.service.auth;

import databreeze.entity.User;

public interface JwtService {
    String generateAccessToken(User user);

    String extractUserId(String token);

    long getExpirationSeconds();
}
