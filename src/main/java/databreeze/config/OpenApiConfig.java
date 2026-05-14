package databreeze.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataBreezeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DataBreeze API - ETL Shopee Viet Nam")
                        .version("MVP-0.1")
                        .description("API backend cho upload Excel/CSV, mapping, import Shopee VN va tinh doanh thu/loi nhuan."))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("/").description("Deploy")
                ));
    }
}