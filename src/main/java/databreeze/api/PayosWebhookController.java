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
@Tag(name = "payOS Webhook", description = "Receive callbacks from payOS")
public class PayosWebhookController {

    @PostMapping("/payos")
    @Operation(summary = "Receive payOS webhook")
    public ApiResponse<Map<String, Object>> handlePayos(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok("Received payOS webhook.", payload);
    }
}
