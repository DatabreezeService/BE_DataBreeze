package databreeze.api;

import databreeze.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Trả lỗi tiếng Việt cho FE.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> notFound(Exception exception) {
        return build(HttpStatus.NOT_FOUND, "KHONG_TIM_THAY", firstNonBlank(exception.getMessage(), "Không tìm thấy dữ liệu yêu cầu."), Map.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, "YEU_CAU_KHONG_HOP_LE", firstNonBlank(exception.getMessage(), "Yêu cầu không hợp lệ."), Map.of());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> forbidden(Exception exception) {
        return build(HttpStatus.FORBIDDEN, "KHONG_CO_QUYEN", firstNonBlank(exception.getMessage(), "Bạn không có quyền thực hiện thao tác này."), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationError(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toVietnameseValidationMessage)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "DU_LIEU_KHONG_HOP_LE", "Dữ liệu gửi lên chưa hợp lệ.", Map.of("loiField", details));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, "THIEU_THAM_SO", "Thiếu tham số bắt buộc: " + exception.getParameterName(), Map.of());
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponse> uploadError(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, "LOI_FILE_UPLOAD", "Không thể upload file. Vui lòng kiểm tra định dạng Excel/CSV và dung lượng file.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> serverError(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "LOI_HE_THONG", "Hệ thống đang gặp lỗi khi xử lý yêu cầu. Vui lòng kiểm tra log backend.", Map.of("exception", exception.getClass().getSimpleName()));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, Map<String, Object> details) {
        ErrorResponse body = new ErrorResponse(false, code, message, status.value(), details, OffsetDateTime.now(ZoneOffset.ofHours(7)));
        return ResponseEntity.status(status).body(body);
    }

    private String toVietnameseValidationMessage(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        return "Trường '" + error.getField() + "': " + (defaultMessage == null ? "không hợp lệ" : defaultMessage);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
