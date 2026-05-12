package databreeze.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Hỗ trợ LocalDate, LocalDateTime, Instant...
        mapper.registerModule(new JavaTimeModule());

        // Không serialize date thành timestamp số
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Nếu AI trả dư field lạ thì không crash
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}