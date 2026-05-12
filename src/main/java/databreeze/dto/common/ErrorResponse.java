package databreeze.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private boolean success;
    private String maLoi;
    private String thongBao;
    private int trangThai;
    private Map<String, Object> chiTiet;
    private OffsetDateTime thoiGian;
}
