package databreeze.service.auth;

import databreeze.service.auth.model.GoogleTokenInfo;

public interface GoogleTokenVerifier {
    GoogleTokenInfo verify(String idToken);
}
