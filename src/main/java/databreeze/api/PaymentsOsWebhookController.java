package databreeze.api;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "PaymentsOS Webhook", description = "Nhan callback tu PaymentsOS")
public class PaymentsOsWebhookController {

    @PostMapping("/paymentsos")
    @Operation(summary = "Nhan webhook PaymentsOS")
    public ApiResponse<Map<String, Object>> handlePaymentsOs(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok("Nhan webhook thanh cong.", payload);
    }
}
