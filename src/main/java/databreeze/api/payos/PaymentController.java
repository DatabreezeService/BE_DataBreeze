package databreeze.api.payos;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.common.ApiResponse;
import databreeze.entity.PaymentTransaction;
import databreeze.entity.PaymentWebhookEvent;
import databreeze.service.payments.PaymentTransactionService;
import databreeze.service.payments.PaymentWebhookEventService;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/v1/payos")
@ConditionalOnProperty(prefix = "app.payos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentController {
    private final PayOS payOS;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentWebhookEventService paymentWebhookEventService;

    public PaymentController(
            PayOS payOS,
            PaymentTransactionService paymentTransactionService,
            PaymentWebhookEventService paymentWebhookEventService) {
        super();
        this.payOS = payOS;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentWebhookEventService = paymentWebhookEventService;
    }

    @PostMapping(path = { "/webhook", "/payos_transfer_handler" })
    public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Map<String, Object> body) {
        PaymentWebhookEvent event = paymentWebhookEventService.recordPayosReceived(body);
        WebhookData data = null;
        try {
            data = payOS.webhooks().verify(body);
            PaymentTransaction transaction = paymentTransactionService.applyPayosWebhook(data, body);
            paymentWebhookEventService.markPayosProcessed(event, data, transaction.getId());
            return ApiResponse.success("Webhook delivered", data);
        } catch (Exception e) {
            if (data == null) {
                paymentWebhookEventService.markPayosInvalid(event);
                throw new IllegalArgumentException("Webhook payOS khong hop le: " + e.getMessage(), e);
            }

            paymentWebhookEventService.markPayosFailed(event, data);
            throw new IllegalStateException("Khong xu ly duoc webhook payOS: " + e.getMessage(), e);
        }
    }
}
