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
import databreeze.dto.payments.PaymentsOsCreatePaymentRequest;
import databreeze.dto.payments.PaymentsOsGenericRequest;
import databreeze.service.payments.PaymentsOsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/paymentsos")
@Tag(name = "PaymentsOS", description = "Cac endpoint mau goi PaymentsOS")
public class PaymentsOsController {

    @Autowired
    private PaymentsOsService paymentsOsService;

    @PostMapping("/payments")
    @Operation(summary = "Tao payment (PaymentsOS)")
    public ApiResponse<String> createPayment(
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PaymentsOsCreatePaymentRequest payload) {
        return ApiResponse.ok("Tao payment thanh cong.", paymentsOsService.createPayment(payload, idempotencyKey));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Lay trang thai payment (PaymentsOS)")
    public ApiResponse<String> getPayment(@PathVariable String paymentId) {
        return ApiResponse.ok("Lay payment thanh cong.", paymentsOsService.getPayment(paymentId));
    }

    @PostMapping("/payments/{paymentId}/captures")
    @Operation(summary = "Capture payment (PaymentsOS)")
    public ApiResponse<String> capturePayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PaymentsOsGenericRequest payload) {
        return ApiResponse.ok("Capture payment thanh cong.",
                paymentsOsService.capturePayment(paymentId, payload, idempotencyKey));
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @Operation(summary = "Refund payment (PaymentsOS)")
    public ApiResponse<String> refundPayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "idempotency-key", required = false) String idempotencyKey,
            @RequestBody PaymentsOsGenericRequest payload) {
        return ApiResponse.ok("Refund payment thanh cong.",
                paymentsOsService.refundPayment(paymentId, payload, idempotencyKey));
    }

    @GetMapping("/payments/{paymentId}/transactions")
    @Operation(summary = "Lay transactions (PaymentsOS)")
    public ApiResponse<String> getTransactions(@PathVariable String paymentId) {
        return ApiResponse.ok("Lay transactions thanh cong.", paymentsOsService.getTransactions(paymentId));
    }
}
