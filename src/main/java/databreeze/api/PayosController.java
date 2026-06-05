package databreeze.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.common.ApiResponse;
import databreeze.dto.payments.PayosCreatePaymentRequest;
import databreeze.dto.payments.PayosGenericRequest;
import databreeze.service.payments.PayosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/payos")
@Tag(name = "payOS", description = "Endpoints for payOS integration")
public class PayosController {

    @Autowired
    private PayosService payosService;

    @PostMapping("/payments")
    @Operation(summary = "Tạo payment (payOS)")
    public ApiResponse<String> createPayment(
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PayosCreatePaymentRequest payload) {
        return ApiResponse.ok("Tạo payment thành công.", payosService.createPayment(payload, idempotencyKey));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Lấy trạng thái payment (payOS)")
    public ApiResponse<String> getPayment(@PathVariable String paymentId) {
        return ApiResponse.ok("Lấy payment thành công.", payosService.getPayment(paymentId));
    }

    @PostMapping("/payments/{paymentId}/captures")
    @Operation(summary = "Capture payment (payOS)")
    public ApiResponse<String> capturePayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PayosGenericRequest payload) {
        return ApiResponse.ok("Capture payment thành công.", payosService.capturePayment(paymentId, payload, idempotencyKey));
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @Operation(summary = "Refund payment (payOS)")
    public ApiResponse<String> refundPayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PayosGenericRequest payload) {
        return ApiResponse.ok("Refund payment thành công.", payosService.refundPayment(paymentId, payload, idempotencyKey));
    }

    @GetMapping("/payments/{paymentId}/transactions")
    @Operation(summary = "Lấy transactions (payOS)")
    public ApiResponse<String> getTransactions(@PathVariable String paymentId) {
        return ApiResponse.ok("Lấy transactions thành công.", payosService.getTransactions(paymentId));
    }
}
