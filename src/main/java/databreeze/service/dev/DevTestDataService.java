package databreeze.service.dev;

import databreeze.dto.dev.SwaggerTestIdsResponse;

/**
 * Service chỉ dùng local để tạo dữ liệu test Swagger.
 */
public interface DevTestDataService {
    SwaggerTestIdsResponse createOrGetSwaggerTestData();
}
