package databreeze.dto.common;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String maLoi,
        String thongBao,
        int trangThai,
        Map<String, Object> chiTiet,
        OffsetDateTime thoiGian
) {}
