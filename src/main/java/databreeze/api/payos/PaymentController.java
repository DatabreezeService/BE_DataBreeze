package databreeze.api.payos;

import databreeze.dto.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/v1/payos")
public class PaymentController {
    private final PayOS payOS;

    public PaymentController(PayOS payOS) {
        super();
        this.payOS = payOS;
    }

    @PostMapping(path = { "/webhook", "/payos_transfer_handler" })
    public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Map<String, Object> body) {
        try {
            WebhookData data = payOS.webhooks().verify(body);
            return ApiResponse.success("Webhook delivered", data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook payOS khong hop le: " + e.getMessage(), e);
        }
    }
}
