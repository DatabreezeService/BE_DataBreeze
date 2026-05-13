package databreeze.service.auth.impl;

import databreeze.service.auth.GoogleTokenVerifier;
import databreeze.service.auth.model.GoogleTokenInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {
    private final RestTemplate restTemplate;

    public GoogleTokenVerifierImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public GoogleTokenInfo verify(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Google token khong hop le.");
        }

        Map<String, Object> payload = response.getBody();
        String email = value(payload.get("email"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google token khong co email.");
        }

        boolean verified = "true".equalsIgnoreCase(value(payload.get("email_verified")));

        return GoogleTokenInfo.builder()
                .subject(value(payload.get("sub")))
                .email(email)
                .name(value(payload.get("name")))
                .picture(value(payload.get("picture")))
                .emailVerified(verified)
                .build();
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }
}
