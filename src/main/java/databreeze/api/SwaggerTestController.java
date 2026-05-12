package databreeze.api;

import databreeze.dto.dev.SwaggerTestIdsResponse;
import databreeze.service.dev.DevTestDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev")
@Profile("local")
@Tag(name = "Hỗ trợ test local", description = "Chỉ bật ở profile local. Tạo user/workspace/store thật trong DB để test ETL trên Swagger.")
public class SwaggerTestController {

    @Autowired
    private DevTestDataService devTestDataService;

    @GetMapping("/swagger-test-ids")
    @Operation(summary = "Tạo/lấy ID test cho 2 luồng: tài khoản cá nhân và workspace chung")
    public SwaggerTestIdsResponse swaggerTestIds() {
        return devTestDataService.createOrGetSwaggerTestData();
    }
}
