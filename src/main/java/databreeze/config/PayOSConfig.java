package databreeze.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    @Bean
    public PayOS payOS(
            @Value("${app.payos.client-id:}") String clientId,
            @Value("${app.payos.api-key:}") String apiKey,
            @Value("${app.payos.checksum-key:}") String checksumKey) {
        if (isBlank(clientId) || isBlank(apiKey) || isBlank(checksumKey)) {
            throw new IllegalStateException("app.payos.client-id, app.payos.api-key, app.payos.checksum-key must be configured");
        }
        return new PayOS(clientId, apiKey, checksumKey);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
