package databreeze.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private OffsetDateTime timestamp;

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String message, T data) {
        return new ApiResponse<>(false, message, data, OffsetDateTime.now());
    }
}
