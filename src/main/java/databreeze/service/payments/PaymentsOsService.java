package databreeze.service.payments;

import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import databreeze.config.PaymentsOsProperties;

@Service
public class PaymentsOsService {
    private final RestTemplate restTemplate;
    private final PaymentsOsProperties properties;

    public PaymentsOsService(RestTemplate restTemplate, PaymentsOsProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String createPayment(Object paymentPayload, String idempotencyKey) {
        return postWithIdempotency("payments", paymentPayload, idempotencyKey);
    }

    public String getPayment(String paymentId) {
        return get("payments/" + paymentId);
    }

    public String capturePayment(String paymentId, Object payload, String idempotencyKey) {
        return postWithIdempotency("payments/" + paymentId + "/captures", payload, idempotencyKey);
    }

    public String refundPayment(String paymentId, Object payload, String idempotencyKey) {
        return postWithIdempotency("payments/" + paymentId + "/refunds", payload, idempotencyKey);
    }

    public String getTransactions(String paymentId) {
        return get("payments/" + paymentId + "/transactions");
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null) {
            return path;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("app-id", properties.getAppId());
        headers.set("private-key", properties.getPrivateKey());
        headers.set("api-version", properties.getApiVersion());
        return headers;
    }

    private String get(String path) {
        String endpointUrl = buildUrl(path);
        HttpHeaders headers = baseHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(endpointUrl, HttpMethod.GET, requestEntity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Loi ket noi den PaymentsOS: " + e.getMessage(), e);
        }
    }

    private String postWithIdempotency(String path, Object payload, String idempotencyKey) {
        String endpointUrl = buildUrl(path);
        HttpHeaders headers = baseHeaders();
        headers.set("idempotency-key", resolveIdempotencyKey(idempotencyKey));
        HttpEntity<Object> requestEntity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.postForObject(endpointUrl, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Loi ket noi den PaymentsOS: " + e.getMessage(), e);
        }
    }

    private String resolveIdempotencyKey(String providedKey) {
        if (providedKey == null || providedKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return providedKey;
    }
}
