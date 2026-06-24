package databreeze.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.payos.PayOS;

@Configuration
@ConditionalOnProperty(prefix = "app.payos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayOSConfig {

    @Bean
    public PayOS payOS(
            @Value("${app.payos.client-id:}") String clientId,
            @Value("${app.payos.api-key:}") String apiKey,
            @Value("${app.payos.checksum-key:}") String checksumKey) {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
