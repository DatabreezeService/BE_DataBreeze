package databreeze.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                        .title("DataBreeze API - ETL Shopee Việt Nam")
                        .version("MVP-0.1")
                        .description("API backend lõi cho tải lên Excel/CSV, gợi ý mapping cột tiếng Việt, xác nhận mapping, import dữ liệu Shopee VN và tính doanh thu/lợi nhuận."))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("MySQL local - test Swagger"),
                        new Server().url("/").description("Môi trường deploy hiện tại")
                ));
    }
}
