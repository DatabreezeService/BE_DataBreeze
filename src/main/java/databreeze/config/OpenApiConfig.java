package databreeze.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataBreezeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DataBreeze API - ETL Shopee Việt Nam")
                        .version("MVP-0.1")
                        .description("API backend lõi cho tải lên Excel/CSV, gợi ý mapping cột tiếng Việt, xác nhận mapping, import dữ liệu Shopee VN và tính doanh thu/lợi nhuận."))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("PostgreSQL local - test Swagger"),
                        new Server().url("/").description("Môi trường deploy hiện tại")
                ));
    }
}
